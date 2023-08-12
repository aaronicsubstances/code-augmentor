import { AstBuilder } from "./AstBuilder";
import { AstFormatter } from "./AstFormatter";
import * as helperUtils from "./helperUtils";
import {
    AugmentedSourceCode,
    AugmentedSourceCodePart,
    AugmentingCodeDescriptor,
    DecoratedLineAstNode,
    EscapedBlockAstNode,
    GeneratedCode,
    GeneratedCodePart,
    GeneratedCodeSectionTransform,
    NestedBlockAstNode,
    SourceCodeAst,
    SourceCodeAstNode,
    UndecoratedLineAstNode
} from "./types";

function findMarkerMatch(markers: string[] | null, marker: any) {
    if (markers) {
        return markers.includes(marker);
    }
    return false;
}

function getFirst<T>(a: T[] | null) {
    if (a && a.length) {
        return a[0];
    }
    return null;
}

function getLast<T>(a: T[] | null) {
    if (a && a.length) {
        return a[a.length - 1];
    }
    return null;
}

export class DefaultAstTransformer {
    augCodeMarkers: string[] | null = null;
    augCodeArgMarkers: string[] | null = null;
    genCodeMarkers: string[] | null = null;

    defaultGenCodeInlineMarker: string | null = null;
    defaultGenCodeStartMarker: string | null = null;
    defaultGenCodeEndMarker: string | null = null;

    static TYPE_AUG_CODE = 1;
    static TYPE_AUG_CODE_ARG = 2;
    static TYPE_GEN_CODE = 3;

    extractAugmentedSourceCode(parentNode: { children: SourceCodeAstNode[] },
            firstLineNumber = 1) {
        const augCodes = new Array<AugmentingCodeDescriptor>();
        const parts = new Array<AugmentedSourceCodePart>();
        const lineCounter = { lineNumber: firstLineNumber };
        for (const n of (parentNode.children || [])) {
            DefaultAstTransformer._addSourceCodeParts(n, parts,
                lineCounter);
        }
        let i = 0;
        while (i < parts.length) {
            const p = parts[i];
            let n = p.node;

            // find start of aug code section
            let augCodeMarkerFound = false;
            if (n.type === AstBuilder.TYPE_DECORATED_LINE ||
                    n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
                const typedNode = n as (DecoratedLineAstNode | EscapedBlockAstNode);
                if (findMarkerMatch(this.augCodeMarkers, typedNode.marker)) {
                    augCodeMarkerFound = true;
                }
            }
            if (!augCodeMarkerFound) {
                i++;
                continue;
            }

            // aug code section found
            const startIdx = i++;

            // find end of aug code section
            while (i < parts.length) {
                n = parts[i].node;
                if (n.type === AstBuilder.TYPE_NESTED_BLOCK_START ||
                    n.type === AstBuilder.TYPE_NESTED_BLOCK_END) {
                    break;
                }
                if (n.type === AstBuilder.TYPE_DECORATED_LINE ||
                        n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
                    const typedNode = n as (DecoratedLineAstNode | EscapedBlockAstNode);
                    if (findMarkerMatch(this.augCodeMarkers, typedNode.marker)) {
                        break;
                    }
                }
                i++;
            }

            augCodes.push(this._createAugCode(parts, startIdx, i))
        }
        const sourceCode: AugmentedSourceCode = {
            parts,
            augCodes
        };
        return sourceCode;
    }

    static _addSourceCodeParts(n: SourceCodeAstNode,
            collector: Array<AugmentedSourceCodePart>,
            counter: { lineNumber: number }) {
        if (n.type === AstBuilder.TYPE_DECORATED_LINE ||
                n.type === AstBuilder.TYPE_UNDECORATED_LINE) {
            collector.push({
                node: n,
                lineNumber: counter.lineNumber++
            });
        }
        else if (n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
            collector.push({
                node: n,
                lineNumber: counter.lineNumber++
            });
            const typedNode = n as EscapedBlockAstNode;
            for (const child of (typedNode.children || [])) {
                // validate type
                if (child.type !== AstBuilder.TYPE_UNDECORATED_LINE) {
                    throw new Error("unexpected child node type for escaped block " +
                        `at line ${counter.lineNumber}: ${child.type}`);
                }
                counter.lineNumber++;
            }
            counter.lineNumber++; // for the end of escape block.
        }
        else if (n.type === AstBuilder.TYPE_NESTED_BLOCK) {
            const typedNode = n as NestedBlockAstNode;
            collector.push({
                node: {
                    type: AstBuilder.TYPE_NESTED_BLOCK_START,
                    marker: typedNode.marker,
                    markerAftermath: typedNode.markerAftermath,
                    lineSep: typedNode.lineSep,
                    indent: typedNode.indent
                } as DecoratedLineAstNode,
                lineNumber: counter.lineNumber++
            });
            for (const child of (typedNode.children || [])) {
                DefaultAstTransformer._addSourceCodeParts(child, collector, counter);
            }
            collector.push({
                node: {
                    type: AstBuilder.TYPE_NESTED_BLOCK_END,
                    marker: typedNode.endMarker,
                    markerAftermath: typedNode.endMarkerAftermath,
                    lineSep: typedNode.endLineSep,
                    indent: typedNode.endIndent
                } as DecoratedLineAstNode,
                lineNumber: counter.lineNumber++
            });
        }
        else {
            throw new Error("unexpected node type " +
                `at line ${counter.lineNumber}: ${n.type}`);
        }
    }

    _createAugCode(sourceCodeParts: Array<AugmentedSourceCodePart>,
            startIdx: number, exclEndIdx: number) {
        const leadPart = sourceCodeParts[startIdx];
        const augCodeData = new Array<string>();
        const augCode : AugmentingCodeDescriptor = {
            leadPartIdx: startIdx,
            partCount: exclEndIdx - startIdx,
            leadPart,
            leadNode: leadPart.node as (DecoratedLineAstNode | EscapedBlockAstNode),
            data: augCodeData,
        };

        // add first data item
        if (augCode.leadNode.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
            const data = this._extractAugCodeArg(
                augCode.leadNode as EscapedBlockAstNode);
            augCodeData.push(data);
        }
        else {
            augCodeData.push(augCode.leadNode.markerAftermath);
        }

        // set leadPart props
        leadPart.augCode = augCode;
        leadPart.type = DefaultAstTransformer.TYPE_AUG_CODE;
        leadPart.data  = augCodeData[0];

        for (let i = startIdx + 1; i < exclEndIdx; i++) {
            const p = sourceCodeParts[i];
            const n = p.node;
            if (n.type !== AstBuilder.TYPE_DECORATED_LINE &&
                    n.type !== AstBuilder.TYPE_ESCAPED_BLOCK) {
                continue;
            }
            const typedNode = n as (DecoratedLineAstNode | EscapedBlockAstNode);
            if (findMarkerMatch(this.genCodeMarkers, typedNode.marker)) {
                p.type = DefaultAstTransformer.TYPE_GEN_CODE;
            }
            else if (findMarkerMatch(this.augCodeArgMarkers, typedNode.marker)) {
                p.type = DefaultAstTransformer.TYPE_AUG_CODE_ARG;
            }
            else {
                continue;
            }
            if (p.type === DefaultAstTransformer.TYPE_AUG_CODE_ARG) {
                let data = '';
                if (typedNode.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
                    data = this._extractAugCodeArg(
                        typedNode as EscapedBlockAstNode);
                }
                else {
                    data = typedNode.markerAftermath;
                }
                p.data = data;
                augCodeData.push(data);
            }
        }
        return augCode;
    }

    _extractAugCodeArg(node: EscapedBlockAstNode) {
        const source = AstFormatter.stringify({
            type: AstBuilder.TYPE_SOURCE_CODE,
            children: node.children
        });
        const builder = new AstBuilder()
        builder.decoratedLineMarkers = this.augCodeArgMarkers
        const sourceNode = builder.parse(source)
        // ensure each line is a decorated
        for (const c of sourceNode.children) {
            if (c.type !== AstBuilder.TYPE_DECORATED_LINE) {
                throw new Error("invalid aug code argument. " +
                    "escaped block must consist of only decorated lines marked by " +
                    "the aug code arg markers supplied to an instance of this class");
            }
        }
        return sourceNode.children.map((v, i) => {
            const n = v as DecoratedLineAstNode;
            let includeTerminator = false;
            if (i < sourceNode.children.length - 1) {
                includeTerminator = true;
            }
            return n.markerAftermath + (includeTerminator ?
                n.lineSep : "");
        }).join("");
    }

    insertGeneratedCode(augmentedSourceCode: AugmentedSourceCode,
            augCode: AugmentingCodeDescriptor,
            genCodes: Array<GeneratedCode | null>) {
        const genCodeSections = augmentedSourceCode.parts
            .filter((x, i) =>
                i >= augCode.leadPartIdx &&
                i < augCode.leadPartIdx + augCode.partCount &&
                x.type === DefaultAstTransformer.TYPE_GEN_CODE);
        let defaultIndent = augCode.leadNode.indent;
        let defaultLineSep = augCode.leadNode.lineSep;

        const genCodeTransforms = this._generateGenCodeTransforms(
            genCodes, genCodeSections, defaultIndent, defaultLineSep);
        DefaultAstTransformer._applyGenCodeTransforms(
            genCodeTransforms, genCodeSections,
            augCode, augmentedSourceCode.parts);
    }

    _generateGenCodeTransforms(
            genCodes: Array<GeneratedCode | null>,
            genCodeSections: AugmentedSourceCodePart[],
            defaultIndent: string,
            defaultLineSep: string) {
        const genCodeTransforms = new Array<GeneratedCodeSectionTransform | null>;
        let minCodeSectionsToDealWith = Math.min(genCodes.length, genCodeSections.length);
        for (let i = 0; i < minCodeSectionsToDealWith; i++) {
            const genCode = genCodes[i];
            if (!genCode) {
                genCodeTransforms.push(null);
                continue;
            }
            const transform: GeneratedCodeSectionTransform = {
                ignoreRemainder: genCode.ignoreRemainder,
                node: null
            };
            if (!genCode.ignore) {
                const genCodeSection = genCodeSections[i];
                const genCodeSectionNode = genCodeSection.node as (EscapedBlockAstNode | DecoratedLineAstNode);
                let genCodeIndent = genCodeSectionNode.indent;
                let genCodeLineSep = genCodeSectionNode.lineSep;
                const genCodeLines = DefaultAstTransformer.extractLinesAndTerminators(
                    genCode.contentParts, genCode.indent,
                    genCode.lineSep || genCodeLineSep);
                const node = this._createGenCodeNode(genCodeLines, genCode,
                    genCodeSection, genCodeIndent, genCodeLineSep);
                transform.node = node;
            }
            genCodeTransforms.push(transform);
        }
        for (let i = minCodeSectionsToDealWith; i < genCodes.length; i++) {
            const genCode = genCodes[i];
            if (!genCode) {
                genCodeTransforms.push(null);
                continue;
            }
            const transform: GeneratedCodeSectionTransform = {
                ignoreRemainder: genCode.ignoreRemainder,
                node: null
            };
            if (!genCode.ignore) {
                const genCodeLines = DefaultAstTransformer.extractLinesAndTerminators(
                    genCode.contentParts, genCode.indent,
                    genCode.lineSep || defaultLineSep);
                const node = this._createGenCodeNode(genCodeLines, genCode,
                    null, defaultIndent, defaultLineSep);
                transform.node = node;
            }
            genCodeTransforms.push(transform);
        }
        return genCodeTransforms;
    }

    static extractLinesAndTerminators(
            contentParts: Array<GeneratedCodePart | null> | null,
            indent: string | null,
            lineSeparator: string | null) {
        if (!contentParts) {
            return [];
        }
        DefaultAstTransformer._repairSplitCrLfs(contentParts);

        const allLines = new Array<string>();
        let lastPartIsExemptAndEmpty = false;
        let lastPartEndedWithLineSep = true;
        for (const part of contentParts) {
            if (!part) {
                continue;
            }
            if (!part.content) {
                if (part.exempt) {
                    lastPartIsExemptAndEmpty = true;
                }
                else {
                    // pass through value of previous lastPartEndedWithLineSep
                }
                continue;
            }
            const splitCode = helperUtils.splitIntoLines(part.content, true);
            for (let j = 0; j < splitCode.length; j+=2) {
                const line = splitCode[j];
                let terminator = splitCode[j + 1];
                if (terminator && lineSeparator && !part.exempt) {
                    terminator = lineSeparator;
                }
                if (j > 0 || lastPartEndedWithLineSep) {
                    // determine indent to apply.
                    // as a policy don't indent blank lines, similar
                    // to what IDEs do.
                    let effectiveIndent = "";
                    if (indent && !(part.exempt || lastPartIsExemptAndEmpty ||
                            helperUtils.isBlank(line))) {
                        effectiveIndent = indent;
                    }

                    // apply indent and add terminator.
                    allLines.push(effectiveIndent + line);
                    allLines.push(terminator);
                }
                else {
                    // don't apply any indent, but rather append to last line,
                    // and replace the empty terminator.
                    if (allLines[allLines.length - 1]) {
                        throw new Error("algorithm failed. expected empty terminator here");
                    }
                    allLines[allLines.length - 2] += line;
                    allLines[allLines.length - 1] = terminator;
                }
            }
            lastPartIsExemptAndEmpty = false;
            lastPartEndedWithLineSep = !!allLines[allLines.length - 1];
        }
        return allLines;
    }

    /**
     * Modifies content parts to remove split CR-LFs, that is, a sequence of 
     * carriage return and line feed which are split across content parts, so
     * that the carriage return character ends a content part, and the following content part
     * starts with the line feed character. 
     * <p>
     * The extractLinesAndTerminators method depend on the absence of
     * split CR-LFs.
     * 
     * @param contentParts content parts to be modified.
     */
    static _repairSplitCrLfs(contentParts: Array<GeneratedCodePart | null>) {
        for (let i = 0; i < contentParts.length - 1; i++) {
            const curr = contentParts[i];
            if (!curr || !curr.content) {
                continue;
            }
            if (curr.content.endsWith("\r")) {
                const next = contentParts[i + 1];
                if (next && next.content && next.content.startsWith("\n")) {
                    // move the \n from next to curr
                    curr.content = curr.content + "\n";
                    next.content = next.content.substring(1);
                }
            }
        }
    }

    _createGenCodeNode(genCodeLines: string[],
            genCode: GeneratedCode,
            genCodeSection: AugmentedSourceCodePart | null,
            defaultIndent: string | null,
            defaultLineSep: string | null): SourceCodeAstNode {
        if (!genCode.markerType || genCode.markerType === AstBuilder.TYPE_ESCAPED_BLOCK) {
            if (genCodeSection && genCodeSection.node.type === genCode.markerType) {
                return AstBuilder.createEscapedNode(genCodeLines, genCodeSection.node);
            }
            if (!this.defaultGenCodeStartMarker) {
                throw new Error("default gen code start marker not set");
            }
            if (!this.defaultGenCodeEndMarker) {
                throw new Error("default gen code end marker not set");
            }
            const attrs: any = {
                indent: defaultIndent,
                lineSep: defaultLineSep,
                marker: this.defaultGenCodeStartMarker,
                endIndent: defaultIndent,
                endLineSep: defaultLineSep,
                endMarker: this.defaultGenCodeEndMarker,
            };
            return AstBuilder.createEscapedNode(genCodeLines, attrs);
        }
        if (genCode.markerType === AstBuilder.TYPE_DECORATED_LINE) {
            if (genCodeSection && genCodeSection.node.type === genCode.markerType) {
                return AstBuilder.createDecoratedLineNode(
                    genCodeLines.join(""), genCodeSection.node);
            }
            if (!this.defaultGenCodeInlineMarker) {
                throw new Error("default gen code inline marker not set");
            }
            const attrs = {
                indent: defaultIndent,
                lineSep: defaultLineSep,
                marker: this.defaultGenCodeInlineMarker
            };
            return AstBuilder.createDecoratedLineNode(
                genCodeLines.join(""), attrs);
        }
        if (genCode.markerType === AstBuilder.TYPE_SOURCE_CODE) {
            const unescapedNode: SourceCodeAst = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [],
            };
            for (let i = 0; i < genCodeLines.length; i+=2) {
                const line = genCodeLines[i];
                const terminator = genCodeLines[i + 1];
                const n: UndecoratedLineAstNode = {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: line,
                    lineSep: terminator
                };
                unescapedNode.children.push(n);
            }
            return unescapedNode;
        }
        throw new Error(`invalid gen code marker type: ${genCode.markerType}`);
    }

    static _applyGenCodeTransforms(
            genCodes: Array<GeneratedCodeSectionTransform | null>,
            genCodeSections: Array<AugmentedSourceCodePart>,
            augCode: AugmentingCodeDescriptor,
            sourceCodeParts: Array<AugmentedSourceCodePart>) {

        const minCodeSectionsToDealWith = Math.min(genCodes.length,
            genCodeSections.length);

        // deal with gen code sections which have to be updated.
        for (let i = 0; i < minCodeSectionsToDealWith; i++) {
            const genCode = genCodes[i];
            const sourceCodePart = genCodeSections[i];
            // update or delete.
            if (!genCode) {
                // delete.
                sourceCodePart.updates = [];
            }
            else if (genCode.node) {
                // update.
                sourceCodePart.updates = [genCode.node];
            }
        }

        // if remainder of gen code sections are not to be ignored,
        // then deal with gen code sections which have to be deleted.
        let ignoreRemainder = false;
        const lastGenCode = getLast(genCodes);
        if (lastGenCode) {
            ignoreRemainder = lastGenCode.ignoreRemainder;
        }
        if (!ignoreRemainder) {
            for (let i = minCodeSectionsToDealWith; i < genCodeSections.length; i++) {
                const sourceCodePart = genCodeSections[i];
                sourceCodePart.updates = [];
            }
        }

        // deal with gen code sections which have to be appended.
        // either use the last gen code section or the last source code
        // part with aug code data within the aug code range.
        let sourceCodePartForAppends = getLast(genCodeSections);
        if (!sourceCodePartForAppends) {
            sourceCodePartForAppends = augCode.leadPart;
            for (let i = augCode.leadPartIdx + augCode.partCount - 1;
                    i > augCode.leadPartIdx; i--) {
                const p = sourceCodeParts[i]
                if (p.type === DefaultAstTransformer.TYPE_AUG_CODE_ARG) {
                    sourceCodePartForAppends = p
                    break
                }
            }
        }
        for (let i = minCodeSectionsToDealWith; i < genCodes.length; i++) {
            const genCode = genCodes[i];
            if (!genCode || !genCode.node) {
                continue;
            }
            if (!sourceCodePartForAppends.updates) {
                sourceCodePartForAppends.updates = [sourceCodePartForAppends.node];
            }
            sourceCodePartForAppends.updates.push(genCode.node);
        }
    }

    /**
     * Uses AugmentedSourceCodePart.updates property to convert list of
     * AugmentedSourceCodePart objects to string. Items in
     * AugmentedSourceCodePart.updates property must be valid nodes acceptable by
     * AstFormatter.stringify() function.
     * To print a source part out without any
     * updates, the updates property should be set to undefined or anything other
     * than an array.
     * To delete a source part, set updates property to an empty
     * array.
     */
    static serializeSourceCodeParts(sourceCodeParts: AugmentedSourceCodePart[]) {
        const children = []
        for (const p of sourceCodeParts) {
            const updates = p.updates;
            if (Array.isArray(updates)) {
                children.push(...updates)
            }
            else {
                children.push(p.node)
            }
        }
        return AstFormatter.stringify({
            type: AstBuilder.TYPE_SOURCE_CODE,
            children
        });
    }
}
