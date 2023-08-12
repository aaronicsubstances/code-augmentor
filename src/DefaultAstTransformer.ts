import { AstBuilder } from "./AstBuilder";
import {
    AugmentingCodeDescriptor,
    DecoratedLineAstNode,
    EscapedBlockAstNode,
    GeneratedCode,
    GeneratedCodePart,
    GeneratedCodeSectionTransform,
    LineObj,
    NestedBlockAstNode,
    SourceCodeAst,
    SourceCodeAstNode,
    UndecoratedLineAstNode
} from "./types";
import * as myutils from "./helperUtils";
import { AstFormatter } from "./AstFormatter";

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

function flattenTree(n: SourceCodeAstNode, collector: Array<LineObj>,
        counter: { lineNumber: number }) {
    if (n.type === AstBuilder.TYPE_DECORATED_LINE ||
            n.type === AstBuilder.TYPE_UNDECORATED_LINE) {
        collector.push({
            node: n,
            type: DefaultAstTransformer.TYPE_OTHER,
            lineNumber: counter.lineNumber++
        });
    }
    else if (n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
        const typedNode = n as EscapedBlockAstNode;
        const children = typedNode.children
        if (children) {
            for (const child of children) {
                // validate type
                if (child.type !== AstBuilder.TYPE_UNDECORATED_LINE) {
                    throw new Error("unexpected child node type for escaped block " +
                        `at line ${counter.lineNumber}: ${child.type}`);
                }
            }
        }
        collector.push({
            node: n,
            type: DefaultAstTransformer.TYPE_OTHER,
            lineNumber: counter.lineNumber
        });
        counter.lineNumber += 2 + (children || []).length;
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
            type: DefaultAstTransformer.TYPE_OTHER,
            lineNumber: counter.lineNumber++
        });
        const children = typedNode.children
        if (children) {
            for (const child of children) {
                flattenTree(child, collector, counter);
            }
        }
        collector.push({
            node: {
                type: AstBuilder.TYPE_NESTED_BLOCK_END,
                marker: typedNode.endMarker,
                markerAftermath: typedNode.endMarkerAftermath,
                lineSep: typedNode.endLineSep,
                indent: typedNode.endIndent
            } as DecoratedLineAstNode,
            type: DefaultAstTransformer.TYPE_OTHER,
            lineNumber: counter.lineNumber++
        });
    }
    else {
        throw new Error("unexpected node type " +
            `at line ${counter.lineNumber}: ${n.type}`);
    }
}

export class DefaultAstTransformer {
    augCodeMarkers: string[] | null = null;
    augCodeArgMarkers: string[] | null = null;
    augCodeJsonArgMarkers: string[] | null = null;
    genCodeMarkers: string[] | null = null;
    defaultGenCodeMarker: string | null = null;

    static TYPE_OTHER = 0;
    static TYPE_AUG_CODE = 1;
    static TYPE_AUG_CODE_ARG = 2;
    static TYPE_GEN_CODE = 3;

    extractAugCodes(parentNode: SourceCodeAst, firstLineNumber = 1) {
        const augCodes = new Array<AugmentingCodeDescriptor>();
        const lineObjects = new Array<LineObj>();
        if (parentNode.children) {
            for (const n of parentNode.children) {
                flattenTree(n, lineObjects, { lineNumber: firstLineNumber });
            }
        }
        let i = 0;
        while (i < lineObjects.length) {
            const lineObj = lineObjects[i];
            const n = lineObj.node;

            // find start of aug code section
            let augCodeMarkerFound = false;
            if (n.type === AstBuilder.TYPE_DECORATED_LINE) {
                const typedNode = n as DecoratedLineAstNode;
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
            while (i < lineObjects.length) {
                if (n.type === AstBuilder.TYPE_NESTED_BLOCK_START ||
                    n.type === AstBuilder.TYPE_NESTED_BLOCK_END) {
                    break;
                }
                if (n.type === AstBuilder.TYPE_DECORATED_LINE) {
                    const typedNode = n as DecoratedLineAstNode;
                    if (findMarkerMatch(this.augCodeMarkers, typedNode.marker)) {
                        break;
                    }
                }
                i++;
            }

            augCodes.push(this._createAugCode(lineObjects, startIdx, i))
        }
        return {
            lineObjects,
            augCodes
        }
    }

    private _createAugCode(lineObjects: Array<LineObj>,
            startIdx: number, exclEndIdx: number) {
        const firstLineObj = lineObjects[startIdx];
        firstLineObj.type = DefaultAstTransformer.TYPE_AUG_CODE;
        const augCodeArgs = [] as any[];
        const augCode : AugmentingCodeDescriptor = {
            startIdx: startIdx,
            exclEndIdx: exclEndIdx,
            lineObj: firstLineObj,
            node: firstLineObj.node as DecoratedLineAstNode,
            args: augCodeArgs,
        };
        for (let i = startIdx + 1; i < exclEndIdx; i++) {
            const lineObj = lineObjects[i];
            const n = lineObj.node;
            if (n.type !== AstBuilder.TYPE_DECORATED_LINE &&
                    n.type !== AstBuilder.TYPE_ESCAPED_BLOCK) {
                continue;
            }
            const typedNode = n as (DecoratedLineAstNode | EscapedBlockAstNode);
            let parseArgAsJson = false;
            if (findMarkerMatch(this.genCodeMarkers, typedNode.marker)) {
                lineObj.type = DefaultAstTransformer.TYPE_GEN_CODE;
            }
            else if (findMarkerMatch(this.augCodeJsonArgMarkers, typedNode.marker)) {
                lineObj.type = DefaultAstTransformer.TYPE_AUG_CODE_ARG;
                parseArgAsJson = true;
            }
            else if (findMarkerMatch(this.augCodeArgMarkers, typedNode.marker)) {
                lineObj.type = DefaultAstTransformer.TYPE_AUG_CODE;
            }
            else {
                continue;
            }
            if (lineObj.type === DefaultAstTransformer.TYPE_AUG_CODE_ARG) {
                let children;
                if (n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
                    children = (n as EscapedBlockAstNode).children;
                }
                else {
                    children = [n];
                }
                const consolidated = AstFormatter.stringify({
                    type: AstBuilder.TYPE_SOURCE_CODE,
                    children
                });
                if (parseArgAsJson) {
                    lineObj.arg = JSON.parse(consolidated);
                }
                else {
                    lineObj.arg = consolidated;
                }
                augCodeArgs.push(lineObj.arg);
            }
        }
        return augCode;
    }

    applyGeneratedCodes(augCode: AugmentingCodeDescriptor,
            lineObjects: LineObj[],
            genCodes: any) {
        genCodes = DefaultAstTransformer._cleanGenCodeList(genCodes);

        const genCodeSections = lineObjects
            .slice(augCode.startIdx, augCode.exclEndIdx)
            .filter(x => x.type === DefaultAstTransformer.TYPE_GEN_CODE);
        let defaultIndent = augCode.node.indent;
        let defaultLineSep = augCode.node.lineSep;

        const genCodeTransforms = this._generateGenCodeTransforms(
            defaultIndent, defaultLineSep, genCodes, genCodeSections);
        DefaultAstTransformer._computeAugCodeTransforms(augCode,
            lineObjects, genCodeSections, genCodeTransforms);
    }

    static _cleanGenCodeList(result: any): Array<GeneratedCode | null> {
        const converted = new Array<GeneratedCode | null>();
        if (Array.isArray(result)) {
            for (const item of result) {
                const genCode = DefaultAstTransformer._convertGenCode(item);
                converted.push(genCode);
            }
        }
        else {
            const genCode = DefaultAstTransformer._convertGenCode(result);
            converted.push(genCode);
        }
        return converted;
    }

    static _convertGenCode(item: any): GeneratedCode | null {
        if (item === null || typeof item === 'undefined') {
            return null;
        }
        if (item.contentParts !== null && typeof item.contentParts !== 'undefined') {
            const contentParts = []
            for (const contentPart of item.contentParts) {
                const elem = DefaultAstTransformer._convertGenCodePart(
                    contentPart);
                if (elem) {
                    contentParts.push(elem);
                }
            }
            return {
                ...item,
                contentParts
            } as GeneratedCode;
        }
        item = DefaultAstTransformer._convertGenCodePart(item);
        return {
            contentParts: item ? [ item ] : null
        } as GeneratedCode;
    }

    static _convertGenCodePart(item: any) {
        if (item === null || typeof item === 'undefined') {
            return null;
        }
        if (item.content !== null && typeof item.content !== 'undefined') {
            return {
                ...item,
                content: `${item.content}`
            } as GeneratedCodePart;
        }
        return {
            content: `${item}`
        } as GeneratedCodePart;
    }

    _generateGenCodeTransforms(
            defaultIndent: string,
            defaultLineSep: string,
            genCodes: Array<GeneratedCode | null>,
            genCodeSections: LineObj[]) {
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
                const genCodeLines = DefaultAstTransformer._extractLinesAndTerminators(
                    genCode.contentParts,
                    genCode.indent, genCode.lineSep || genCodeLineSep);
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
                const genCodeLines = DefaultAstTransformer._extractLinesAndTerminators(
                    genCode.contentParts,
                    genCode.indent, genCode.lineSep || defaultLineSep);
                const node = this._createGenCodeNode(genCodeLines, genCode,
                    null, defaultIndent, defaultLineSep);
                transform.node = node;
            }
            genCodeTransforms.push(transform);
        }
        return genCodeTransforms;
    }

    static _extractLinesAndTerminators(
            contentParts: GeneratedCodePart[] | null,
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
            const splitCode = myutils.splitIntoLines(part.content, true);
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
                    if (!(!indent || part.exempt || lastPartIsExemptAndEmpty ||
                            myutils.isBlank(line))) {
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
    static _repairSplitCrLfs(contentParts: GeneratedCodePart[]) {
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
            genCodeSection: LineObj | null,
            defaultIndent: string | null,
            defaultLineSep: string | null): SourceCodeAstNode {
        if (genCode.markerType === AstBuilder.TYPE_DECORATED_LINE) {
            if (genCodeSection && genCodeSection.node.type === genCode.markerType) {
                return AstBuilder.createDecoratedLineNode(
                    genCodeLines.join(""), genCodeSection.node);
            }
            if (!this.defaultGenCodeMarker) {
                throw new Error("default gen code marker not set");
            }
            const attrs = {
                indent: defaultIndent,
                lineSep: defaultLineSep,
                marker: this.defaultGenCodeMarker
            };
            return AstBuilder.createDecoratedLineNode(
                genCodeLines.join(""), attrs);
        }
        if (genCode.markerType === AstBuilder.TYPE_ESCAPED_BLOCK) {
            if (genCodeSection && genCodeSection.node.type === genCode.markerType) {
                return AstBuilder.createEscapedNode(genCodeLines, genCodeSection.node);
            }
            if (!this.defaultGenCodeMarker) {
                throw new Error("default gen code marker not set");
            }
            const attrs: any = {
                indent: defaultIndent,
                lineSep: defaultLineSep,
                marker: this.defaultGenCodeMarker,
                endIndent: defaultIndent,
                endLineSep: defaultLineSep,
                endMarker: this.defaultGenCodeMarker,
            };
            return AstBuilder.createEscapedNode(genCodeLines, attrs);
        }
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

    static _computeAugCodeTransforms(
            augCode: AugmentingCodeDescriptor,
            lineObjects: Array<LineObj>,
            genCodeSections: Array<LineObj>,
            genCodes: Array<GeneratedCodeSectionTransform | null>) {

        const minCodeSectionsToDealWith = Math.min(genCodes.length, genCodeSections.length);

        // deal with gen code sections which have to be updated.
        for (let i = 0; i < minCodeSectionsToDealWith; i++) {
            const genCode = genCodes[i];
            const lineObj = genCodeSections[i];
            // update or delete.
            if (!genCode) {
                // delete.
                lineObj.updates = [];
            }
            else if (genCode.node) {
                // update.
                lineObj.updates = [genCode.node];
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
                const lineObj = genCodeSections[i];
                lineObj.updates = [];
            }
        }

        // deal with gen code sections which have to be appended.
        // either use the last gen code section or the last line object
        // in aug code.
        let lineObjForAppends = getLast(genCodeSections);
        if (!lineObjForAppends) {
            lineObjForAppends = lineObjects[augCode.exclEndIdx - 1];                    
        }
        for (let i = minCodeSectionsToDealWith; i < genCodes.length; i++) {
            const genCode = genCodes[i];
            if (!genCode || !genCode.node) {
                continue;
            }
            if (!lineObjForAppends.updates) {
                lineObjForAppends.updates = [lineObjForAppends.node];
            }
            lineObjForAppends.updates.push(genCode.node);
        }
    }

    /**
     * Applies insert/update/delete operations to line objects as
     * determined by LineObj.updates property. Contents of LineObj.updates
     * property must be valid nodes acceptable by AstFormatter.stringify
     */
    static performTransformations(lineObjects: LineObj[]) {
        const children = []
        for (const lineObj of lineObjects) {
            const updates = lineObj.updates;
            if (updates === null || typeof updates === "undefined") {
                children.push(lineObj.node)
            }
            else {
                children.push(...updates)
            }
        }
        return AstFormatter.stringify({
            type: AstBuilder.TYPE_SOURCE_CODE,
            children
        });
    }
}
