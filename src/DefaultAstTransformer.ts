import { AstParser } from "./AstParser";
import { AstFormatter } from "./AstFormatter";
import * as helperUtils from "./helperUtils";
import {
    AugmentedSourceCode,
    AugmentedSourceCodePart,
    AugmentingCodeDescriptor,
    DecoratedLineAstNode,
    EscapedBlockAstNode,
    GeneratedCode,
    GeneratedCodeOptions,
    NestedBlockAstNode,
    SourceCodeAst,
    SourceCodeAstNode,
    UndecoratedLineAstNode
} from "./types";

export interface GeneratedCodeSectionTransform { // internally exposed for testing
    node: SourceCodeAstNode | null; // null means ignore
    ignoreRemainder: boolean;
}

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
            if (n.type === AstParser.TYPE_DECORATED_LINE ||
                    n.type === AstParser.TYPE_ESCAPED_BLOCK) {
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
                if (n.type === AstParser.TYPE_NESTED_BLOCK_START ||
                        n.type === AstParser.TYPE_NESTED_BLOCK_END) {
                    break;
                }
                if (n.type === AstParser.TYPE_DECORATED_LINE ||
                        n.type === AstParser.TYPE_ESCAPED_BLOCK) {
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
        if (n.type === AstParser.TYPE_DECORATED_LINE ||
                n.type === AstParser.TYPE_UNDECORATED_LINE) {
            collector.push({
                node: n,
                lineNumber: counter.lineNumber++
            });
        }
        else if (n.type === AstParser.TYPE_ESCAPED_BLOCK) {
            collector.push({
                node: n,
                lineNumber: counter.lineNumber++
            });
            const typedNode = n as EscapedBlockAstNode;
            for (const child of (typedNode.children || [])) {
                // validate type
                if (child.type !== AstParser.TYPE_UNDECORATED_LINE) {
                    throw new Error("unexpected child node type for escaped block " +
                        `at line ${counter.lineNumber}: ${child.type}`);
                }
                counter.lineNumber++;
            }
            counter.lineNumber++; // for the end of escape block.
        }
        else if (n.type === AstParser.TYPE_NESTED_BLOCK) {
            const typedNode = n as NestedBlockAstNode;
            collector.push({
                node: {
                    type: AstParser.TYPE_NESTED_BLOCK_START,
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
                    type: AstParser.TYPE_NESTED_BLOCK_END,
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
        const augCodeData = new Array<string>();
        const augCode : AugmentingCodeDescriptor = {
            leadPartIdx: startIdx,
            partCount: exclEndIdx - startIdx,
            leadPart: null as any,
            leadNode: null as any,
            data: augCodeData,
        };

        for (let i = startIdx; i < exclEndIdx; i++) {
            const p = sourceCodeParts[i];
            const n = p.node;
            if (n.type !== AstParser.TYPE_DECORATED_LINE &&
                    n.type !== AstParser.TYPE_ESCAPED_BLOCK) {
                continue;
            }
            const typedNode = n as (DecoratedLineAstNode | EscapedBlockAstNode);
            if (i > startIdx) {
                if (findMarkerMatch(this.genCodeMarkers, typedNode.marker)) {
                    p.type = DefaultAstTransformer.TYPE_GEN_CODE;
                }
                else if (findMarkerMatch(this.augCodeArgMarkers, typedNode.marker)) {
                    p.type = DefaultAstTransformer.TYPE_AUG_CODE_ARG;
                }
                else {
                    continue;
                }
            }
            else {
                p.type = DefaultAstTransformer.TYPE_AUG_CODE;
                p.augCode = augCode;
                augCode.leadPart = p;
                augCode.leadNode = p.node as (DecoratedLineAstNode | EscapedBlockAstNode);
            }
            if (p.type === DefaultAstTransformer.TYPE_AUG_CODE ||
                    p.type === DefaultAstTransformer.TYPE_AUG_CODE_ARG) {
                let data = '';
                if (typedNode.type === AstParser.TYPE_ESCAPED_BLOCK) {
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
            type: AstParser.TYPE_SOURCE_CODE,
            children: node.children
        });
        const builder = new AstParser();
        builder.decoratedLineMarkers = this.augCodeArgMarkers;
        const sourceNode = builder.parse(source);
        return sourceNode.children.map((v, i) => {
            let prefix = '', suffix = '';
            if (v.type === AstParser.TYPE_DECORATED_LINE) {
                prefix = (v as DecoratedLineAstNode).markerAftermath;
            }
            else {
                prefix = (v as UndecoratedLineAstNode).text;
            }
            if (i < sourceNode.children.length - 1) {
                suffix = (v as DecoratedLineAstNode | UndecoratedLineAstNode).lineSep;
            }
            return prefix + suffix;
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
                    genCode.contentParts,
                    genCode.indent || genCodeIndent,
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
                    genCode.contentParts,
                    genCode.indent || defaultIndent,
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
            contentParts: Array<string | GeneratedCodeOptions | null> | null,
            defaultIndent: string | null,
            defautLineSep: string | null) {
        if (!contentParts) {
            return [];
        }

        const indentedParts = new Array<string>();
        const defaultOptions: GeneratedCodeOptions = {
            exempt: false,
            indent: defaultIndent,
            lineSep: defautLineSep
        };

        for (let i = 0; i < contentParts.length; i++) {
            const part = contentParts[i];
            let content = '';
            if (typeof part === "string") {
                content = part;
            }
            else {
                continue;
            }
            let effectiveOptions = defaultOptions;
            let effectiveIndentOptionWasExplicitlyGiven = false;
            if (i > 0) {
                const previousPart = contentParts[i -1];
                if (typeof previousPart !== "string" && previousPart) {
                    effectiveOptions = {
                        exempt: !!previousPart.exempt,
                        indent: defaultOptions.indent,
                        lineSep: defaultOptions.lineSep
                    };
                    if (previousPart.indent || previousPart.indent === "") {
                        effectiveOptions.indent = previousPart.indent;
                        effectiveIndentOptionWasExplicitlyGiven = true;
                    }
                    if (previousPart.lineSep || previousPart.lineSep === "") {
                        effectiveOptions.lineSep = previousPart.lineSep;
                    }
                }
            }
            if (effectiveOptions.exempt) {
                indentedParts.push(content);
                continue;
            }
            const isLastContentPart = i === contentParts.length - 1;
            let splitCode = ["", ""];
            if (content) {
                splitCode = helperUtils.splitIntoLines(content, true);
            }
            for (let j = 0; j < splitCode.length; j+=2) {
                const linePortion = splitCode[j];
                let terminator = splitCode[j + 1];

                // determine indent to apply.
                if (effectiveOptions.indent) {
                    if (effectiveIndentOptionWasExplicitlyGiven) {
                        indentedParts.push(effectiveOptions.indent);
                    }
                    else {
                        // try not to indent portions of lines ending on line terminators
                        // or end of input,
                        // similar to what IDEs do by not indenting blank lines.
                        // But let any explicit option override this.
                        if (terminator || isLastContentPart) {
                            if (!helperUtils.isBlank(linePortion)) {
                                indentedParts.push(effectiveOptions.indent);
                            }
                        }
                        else {
                            indentedParts.push(effectiveOptions.indent);
                        }
                    }
                }

                indentedParts.push(linePortion);

                // determine terminator to use.
                if (terminator) {
                    indentedParts.push(effectiveOptions.lineSep ?? terminator);
                }
            }
        }
        return helperUtils.splitIntoLines(indentedParts.join(""), true);
    }

    _createGenCodeNode(genCodeLines: string[],
            genCode: GeneratedCode,
            genCodeSection: AugmentedSourceCodePart | null,
            defaultIndent: string | null,
            defaultLineSep: string | null): SourceCodeAstNode {
        if (!genCode.markerType || genCode.markerType === AstParser.TYPE_ESCAPED_BLOCK) {
            if (genCodeSection && genCodeSection.node.type === genCode.markerType) {
                return AstParser.createEscapedNode(genCodeLines, genCodeSection.node);
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
            return AstParser.createEscapedNode(genCodeLines, attrs);
        }
        if (genCode.markerType === AstParser.TYPE_DECORATED_LINE) {
            if (genCodeSection && genCodeSection.node.type === genCode.markerType) {
                return AstParser.createDecoratedLineNode(
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
            return AstParser.createDecoratedLineNode(
                genCodeLines.join(""), attrs);
        }
        if (genCode.markerType === AstParser.TYPE_SOURCE_CODE) {
            const unescapedNode: SourceCodeAst = {
                type: AstParser.TYPE_SOURCE_CODE,
                children: [],
            };
            for (let i = 0; i < genCodeLines.length; i+=2) {
                const line = genCodeLines[i];
                const terminator = genCodeLines[i + 1];
                const n: UndecoratedLineAstNode = {
                    type: AstParser.TYPE_UNDECORATED_LINE,
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
            type: AstParser.TYPE_SOURCE_CODE,
            children
        });
    }
}
