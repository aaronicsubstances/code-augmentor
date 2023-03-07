import AstBuilder from "./AstBuilder";
import {
    AugmentingCodeDescriptor,
    DecoratedLineAstNode,
    DefaultAstTransformSpec,
    EscapedBlockAstNode,
    GeneratedCode,
    GeneratedCodeDescriptor,
    GeneratedCodePart,
    GeneratedCodeSectionTransform,
    NestedBlockAstNode,
    SourceCodeAst,
    SourceCodeAstNode
} from "./types";
import * as myutils from "./myutils";

export default class DefaultAstTransformer {
    augCodeMarkers: string[] | null = null;
    augCodeArgMarkers: string[] | null = null;
    augCodeJsonArgMarkers: string[] | null = null;
    augCodeArgSepMarkers: string[] | null = null;
    genCodeMarkers: string[] | null = null;
    defaultGenCodeInlineMarker: string | null = null;
    defaultGenCodeStartMarker: string | null = null;
    defaultGenCodeEndMarker: string | null = null;

    extractAugCodes(parentNode: SourceCodeAst, firstLineNumber = 1) {
        const augCodes = new Array<AugmentingCodeDescriptor>();
        this._addAugCodes(parentNode, null, { consumedLineCount: firstLineNumber - 1 }, augCodes);
        return augCodes;
    }

    private _addAugCodes(parentNode: SourceCodeAst | NestedBlockAstNode,
            parentAugCode: AugmentingCodeDescriptor | null,
            lineCounter: { consumedLineCount: number },
            dest: AugmentingCodeDescriptor[]) {
        const src = parentNode.children;
        if (!src) {
            return;
        }
        for (let i = 0; i < src.length; i++) {
            const n = src[i];
            if (n.type === AstBuilder.TYPE_NESTED_BLOCK) {
                const typedNode = n as NestedBlockAstNode;
                if (!findMarkerMatch(this.augCodeMarkers, typedNode.marker)) {
                    lineCounter.consumedLineCount += getLineCount(n);
                    continue;
                }
                lineCounter.consumedLineCount++;
                const augCodeArgs = this._extractAugCodeArgs(typedNode, 0);
                const augCodeEndArgs= this._extractAugCodeArgs(parentNode, i + 1);
                const augCode : AugmentingCodeDescriptor = {
                    parentNode: parentNode,
                    idxInParentNode: i,
                    nestedBlockUsed: true,
                    lineNumber: lineCounter.consumedLineCount,
                    markerAftermath: typedNode.markerAftermath,
                    args: augCodeArgs.args,
                    argsExclEndIdxInParentNode: augCodeArgs.exclEndIdx,
                    endMarkerAftermath: typedNode.endMarkerAftermath,
                    endArgs: augCodeEndArgs.args,
                    endArgsExclEndIdxInParentNode: augCodeEndArgs.exclEndIdx,
                    parent: parentAugCode,
                    children: []
                };
                dest.push(augCode);
                this._addAugCodes(typedNode, augCode, lineCounter, augCode.children);
                lineCounter.consumedLineCount++;
            }
            else if (n.type === AstBuilder.TYPE_DECORATED_LINE) {
                lineCounter.consumedLineCount++;
                const typedNode = n as DecoratedLineAstNode;
                if (!findMarkerMatch(this.augCodeMarkers, typedNode.marker)) {
                    continue;
                }
                const augCodeArgs = this._extractAugCodeArgs(parentNode, i + 1);
                const augCode : AugmentingCodeDescriptor = {
                    parentNode: parentNode,
                    idxInParentNode: i,
                    nestedBlockUsed: false,
                    lineNumber: lineCounter.consumedLineCount,
                    markerAftermath: typedNode.markerAftermath,
                    args: augCodeArgs.args,
                    argsExclEndIdxInParentNode: augCodeArgs.exclEndIdx,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parent: parentAugCode,
                    children: []
                };
                dest.push(augCode);
            }
            else {
                lineCounter.consumedLineCount += getLineCount(n);
            }
        }
    }

    _extractAugCodeArgs(parentNode: SourceCodeAst | NestedBlockAstNode, startIndex: number) {
        // get all arg nodes immediately following aug code.
        let args = new Array<any>();
        const src = parentNode.children;
        if (!src) {
            return {
                exclEndIdx: 0,
                args: args
            };
        }
        let i;
        for (i = startIndex; i < src.length; i++) {
            const n = src[i];
            if (n.type != AstBuilder.TYPE_DECORATED_LINE) {
                break;
            }
            const typedNode = n as DecoratedLineAstNode;
            if (findMarkerMatch(this.augCodeArgSepMarkers, typedNode.marker)) {
                args.push(null);
            }
            else if (findMarkerMatch(this.augCodeJsonArgMarkers, typedNode.marker)) {
                args.push(typedNode.markerAftermath);
                args.push(typedNode.lineSep);
                args.push(true);
            }
            else if (findMarkerMatch(this.augCodeArgMarkers, typedNode.marker)) {
                args.push(typedNode.markerAftermath);
                args.push(typedNode.lineSep);
                args.push(false);
            }
            else {
                break;
            }
        }
        args = DefaultAstTransformer._consolidateAugCodeArgs(args);
        return {
            exclEndIdx: i,
            args: args
        };
    }

    static _consolidateAugCodeArgs(args: any[]) {
        const blocks = new Array<any>();
        let lastBlockIsJson: boolean | null = null;
        const block = new Array<any>();
        let startIdx = 0;
        while (startIdx < args.length) {
            if (args[startIdx] === null) {
                lastBlockIsJson = null;
                startIdx++;
                continue;
            }
            // clear block items
            block.length = 0;
            let j;
            for ( j = startIdx; j < args.length; j+=3) {
                const lineContent = args[j];
                if (lineContent === null) {
                    break;
                }
                const jsonify = args[j + 2] as boolean;
                if (lastBlockIsJson === null) {
                    lastBlockIsJson = jsonify;
                }
                else if (lastBlockIsJson !== jsonify) {
                    break;
                }
                if (j > startIdx) {
                    // add prev line sep 
                    block.push(args[j - 3 + 1]);
                }
                block.push(lineContent);
            }
            if (lastBlockIsJson) {
                blocks.push(JSON.parse(block.join("")));
            }
            else {
                blocks.push(block.join(""));
            }
            lastBlockIsJson = !lastBlockIsJson;
            startIdx = j;
        }

        return blocks;
    }

    applyGeneratedCodes(augCode: AugmentingCodeDescriptor,
            genCodes: GeneratedCode[]) {
        const augCodeNode = augCode.parentNode.children[augCode.idxInParentNode] as 
            (NestedBlockAstNode | DecoratedLineAstNode);
        const genCodeTransforms = new Array<GeneratedCodeSectionTransform | null>();
        const genCodeSections = this.extractGenCodeSections(augCode);

        // begin by processing all but the last gen code.
        let genCodeSectionReduction = 0;
        if (augCode.nestedBlockUsed) {
            const lastGenCodeSection = getLast(genCodeSections);
            if (lastGenCodeSection && lastGenCodeSection.parentNode === augCode.parentNode) {
                genCodeSectionReduction++;
            }
        }
        let defaultIndent = augCodeNode.indent;
        let defaultLineSep = augCodeNode.lineSep;
        this._addGenCodeTransforms(
            defaultIndent, defaultLineSep,
            genCodes.slice(0, -1),
            genCodeSections.slice(0, genCodeSections.length - genCodeSectionReduction),
            genCodeTransforms);

        // now process last gen code.
        if (genCodeSectionReduction) {
            defaultIndent = (augCodeNode as NestedBlockAstNode).endIndent;
            defaultLineSep = (augCodeNode as NestedBlockAstNode).endLineSep;
        }
        this._addGenCodeTransforms(defaultIndent, defaultLineSep,
            genCodes.slice(-1),
            genCodeSections.slice(genCodeSections.length - genCodeSectionReduction),
            genCodeTransforms);
        const augCodeTransforms = this.computeAugCodeTransforms(augCode,
            genCodeTransforms, genCodeSections);
        DefaultAstTransformer.performTransformations(augCodeTransforms);
    }

    _addGenCodeTransforms(
            defaultIndent: string,
            defaultLineSep: string,
            genCodes: GeneratedCode[],
            genCodeSections: GeneratedCodeDescriptor[],
            genCodeTransforms: Array<GeneratedCodeSectionTransform | null>) {
        let minCodeSectionsToDealWith = Math.min(genCodes.length, genCodeSections.length);
        for (let i = 0; i < minCodeSectionsToDealWith; i++) {
            const genCode = genCodes[i];
            if (!genCode) {
                genCodeTransforms.push(null);
                continue;
            }
            const transform: GeneratedCodeSectionTransform = {
                ignore: genCode.ignore,
                ignoreRemainder: genCode.ignoreRemainder,
                node: null
            };
            if (!transform.ignore) {
                const genCodeSection = genCodeSections[i];
                const genCodeSectionNode = genCodeSection.parentNode.children[
                    genCodeSection.idxInParentNode] as (EscapedBlockAstNode | DecoratedLineAstNode);
                let genCodeIndent = genCodeSectionNode.indent;
                let genCodeLineSep = genCodeSectionNode.lineSep;
                const genCodeLines = DefaultAstTransformer.extractLinesAndTerminators(
                    genCode.contentParts,
                    genCode.indent, genCodeLineSep);
                const node = this._createGenCodeNode(genCodeLines,
                    genCode.useInlineMarker,
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
                ignore: genCode.ignore,
                ignoreRemainder: genCode.ignoreRemainder,
                node: null
            };
            if (!transform.ignore) {
                const genCodeLines = DefaultAstTransformer.extractLinesAndTerminators(
                    genCode.contentParts,
                    genCode.indent, defaultLineSep);
                const node = this._createGenCodeNode(genCodeLines, genCode.useInlineMarker,
                    null, defaultIndent, defaultLineSep);
                transform.node = node;
            }
            genCodeTransforms.push(transform);
        }
    }

    extractGenCodeSections(augCode: AugmentingCodeDescriptor) {
        const genCodeSections = new Array<GeneratedCodeDescriptor>();
        if (augCode.nestedBlockUsed) {
            this._addNestedGenCodeSections(augCode, genCodeSections);
        }
        const lastGenCodeSection = this._getLastGenCodeSection(augCode);
        if (lastGenCodeSection) {
            genCodeSections.push(lastGenCodeSection);
        } 
        return genCodeSections;
    }

    private _addNestedGenCodeSections(augCode: AugmentingCodeDescriptor,
            genCodeSections: GeneratedCodeDescriptor[]) {
        // get all gen code nodes except those functioning as
        // last gen code sections for child aug codes.
        const exemptions = augCode.children
            .map(a => {
                const g = this._getLastGenCodeSection(a);
                if (g) {
                    return g.idxInParentNode;
                }
                return -1;
            });
        const augCodeNode = augCode.parentNode.children[
            augCode.idxInParentNode] as NestedBlockAstNode;
        for (let i = 0; i < augCodeNode.children.length; i++) {
            if (exemptions.includes(i)) {
                continue;
            }
            const n = augCodeNode.children[i];
            if (n.type === AstBuilder.TYPE_DECORATED_LINE ||
                    n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
                const typedNode = n as (DecoratedLineAstNode | EscapedBlockAstNode);
                if (findMarkerMatch(this.genCodeMarkers, typedNode.marker)) {
                    const genCodeSection: GeneratedCodeDescriptor = {
                        parentNode: augCodeNode,
                        idxInParentNode: i,
                        nestedBlockUsed: n.type === AstBuilder.TYPE_NESTED_BLOCK
                    };
                    genCodeSections.push(genCodeSection);
                }
            }
        }
    }

    _getLastGenCodeSection(augCode: AugmentingCodeDescriptor) {
        // get first one below aug code's args before another aug code arg or
        // another aug code.
        const startIdx = augCode.nestedBlockUsed ?
            augCode.endArgsExclEndIdxInParentNode :
            augCode.argsExclEndIdxInParentNode;
        const nodes = augCode.parentNode.children;
        for (let i = startIdx; i < nodes.length; i++) {
            const n = nodes[i];
            // look for aug code markers.
            if ((n.type === AstBuilder.TYPE_DECORATED_LINE ||
                    n.type === AstBuilder.TYPE_NESTED_BLOCK)) {
                const typedNode = n as (DecoratedLineAstNode | NestedBlockAstNode);
                if (findMarkerMatch(this.augCodeMarkers, typedNode.marker)) {
                    break;
                }
            }
            // look for aug code arg markers.
            if (n.type === AstBuilder.TYPE_DECORATED_LINE) {
                const typedNode = n as DecoratedLineAstNode;
                if (findMarkerMatch(this.augCodeJsonArgMarkers, typedNode.marker) ||
                        findMarkerMatch(this.augCodeArgMarkers, typedNode.marker) ||
                        findMarkerMatch(this.augCodeArgSepMarkers, typedNode.marker)) {
                    break;
                }
            }
            // look for gen code markers.
            if (n.type === AstBuilder.TYPE_DECORATED_LINE ||
                    n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
                const typedNode = n as (DecoratedLineAstNode | EscapedBlockAstNode);
                if (findMarkerMatch(this.genCodeMarkers, typedNode.marker)) {
                    const genCodeSection: GeneratedCodeDescriptor = {
                        parentNode: augCode.parentNode,
                        idxInParentNode: i,
                        nestedBlockUsed: n.type === AstBuilder.TYPE_NESTED_BLOCK
                    };
                    return genCodeSection;
                }
            }
        }
        return null;
    }

    static extractLinesAndTerminators(
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
        let lastPartIsExempt = false;
        for (const part of contentParts) {
            if (!part) {
                continue;
            }
            if (!part.content) {
                if (part.exempt) {
                    lastPartIsExemptAndEmpty = true;
                    lastPartIsExempt = true;
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
            lastPartIsExempt = part.exempt;
            lastPartIsExemptAndEmpty = false;
            lastPartEndedWithLineSep = !!allLines[allLines.length - 1];
        }

        // differentiate between empty content parts, and
        // content parts with empty contents.
        if (contentParts.length > 0) {
            if (allLines.length === 0) {
                allLines.push("");
                allLines.push("");
            }

            // ensure ending terminator, but only if line separator was given,
            // so as to provide a way to skip appending of ending newlines.
            if (lineSeparator && !lastPartIsExempt) {
                allLines[allLines.length - 1] = lineSeparator;
            }
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

    /**
     * Determines the list of transformations that will keep an aug code section in
     * sync with some specified list of gen code sections.
     * <p>
     * By accepting any gen code sections, this method is tolerating wrong
     * gen code sections. To make sense of them, this method simply syncs
     * nested gen code sections with all but the last gen code,
     * and syncs the last gen code with the first non-nested gen code section.
     * <p>It must be mentioned that this behaviour works perfectly for the correct
     * case of gen code sections properly aligned with their aug code sections.
     * @param genCodes
     * @param augCode
     * @param genCodeSections
     * @returns
     */
    computeAugCodeTransforms(
            augCode: AugmentingCodeDescriptor,
            genCodes: Array<GeneratedCodeSectionTransform | null>,
            genCodeSections: GeneratedCodeDescriptor[] | null) {
        const augCodeNode = augCode.parentNode.children[augCode.idxInParentNode];
        if (!genCodeSections) {
            genCodeSections = this.extractGenCodeSections(augCode);
        }
        const transforms = new Array<DefaultAstTransformSpec>();
        const nestedGenCodeSections = new Array<GeneratedCodeDescriptor>();
        const nonNestedGenCodeSections = new Array<GeneratedCodeDescriptor>();
        for (const genCodeSection of genCodeSections) {
            if (genCodeSection.parentNode === augCodeNode) {
                // don't mind if another nested aug code is the correct owner.
                nestedGenCodeSections.push(genCodeSection);
            }
            else {
                // don't mind if gen code is unrelated to aug code.
                nonNestedGenCodeSections.push(genCodeSection);
            }
        }
        if (augCode.nestedBlockUsed) {
            this._addAugCodeTransforms(genCodes.slice(0, -1), augCode,
                nestedGenCodeSections, augCodeNode as NestedBlockAstNode, transforms);
        }
        else {
            // not needed for correct gen code sections.
            this._addAugCodeTransforms([], augCode,
                nestedGenCodeSections, augCode.parentNode, transforms);
        }
        // ignore all but the last gen code for non nested sections.
        this._addAugCodeTransforms(genCodes.slice(-1), augCode,
            nonNestedGenCodeSections, augCode.parentNode, transforms);
        return transforms;
    }

    _addAugCodeTransforms(genCodes: Array<GeneratedCodeSectionTransform | null>,
            augCode: AugmentingCodeDescriptor,
            genCodeSections: GeneratedCodeDescriptor[],
            targetNode: SourceCodeAst | NestedBlockAstNode,
            transformSpecs: DefaultAstTransformSpec[]) {
        const minCodeSectionsToDealWith = Math.min(genCodes.length, genCodeSections.length);

        // deal with gen code sections which have to be updated.
        for (let i = 0; i < minCodeSectionsToDealWith; i++) {
            const genCode = genCodes[i];
            const genCodeSection = genCodeSections[i];
            // update or delete.
            if (!genCode) {
                // delete.
                const transformSpec: DefaultAstTransformSpec = {
                    node: targetNode,
                    childIndex: genCodeSection.idxInParentNode,
                    childToInsert: null,
                    replacementChild: null,
                    performDeletion: true
                };
                transformSpecs.push(transformSpec);
            }
            else if (!genCode.ignore) {
                // update.
                const transformSpec: DefaultAstTransformSpec = {
                    node: targetNode,
                    childIndex: genCodeSection.idxInParentNode,
                    childToInsert: null,
                    replacementChild: genCode.node,
                    performDeletion: false
                };
                transformSpecs.push(transformSpec);
            }
        }

        // if remainder of gen code sections are not to be ignored,
        // then deal with gen code sections which have to be deleted.
        let ignoreRemainder = false;
        if (genCodes.length > 0 && genCodes.length < minCodeSectionsToDealWith) {
            const g = getLast(genCodes);
            if (g) {
                ignoreRemainder = g.ignoreRemainder;
            }
        }
        if (!ignoreRemainder) {
            for (let i = minCodeSectionsToDealWith; i < genCodeSections.length; i++) {
                const genCodeSection = genCodeSections[i];
                const transformSpec: DefaultAstTransformSpec = {
                    node: targetNode,
                    childIndex: genCodeSection.idxInParentNode,
                    childToInsert: null,
                    replacementChild: null,
                    performDeletion: true
                };
                transformSpecs.push(transformSpec);
            }
        }

        // deal with gen code sections which have to be appended.
        let targetIndexForInsertions = -1;
        for (let i = minCodeSectionsToDealWith; i < genCodes.length; i++) {
            const genCode = genCodes[i];
            if (!genCode || genCode.ignore) {
                continue;
            }
            if (targetIndexForInsertions == -1) {
                const g = getLast(genCodeSections);
                if (g) {
                    targetIndexForInsertions = g.idxInParentNode + 1;                    
                }
                else {
                    if (augCode.nestedBlockUsed) {
                        if (targetNode === augCode.parentNode.children[augCode.idxInParentNode]) {
                            targetIndexForInsertions = augCode.argsExclEndIdxInParentNode;
                        }
                        else {
                            targetIndexForInsertions = augCode.endArgsExclEndIdxInParentNode;
                        }
                    }
                    else {
                        targetIndexForInsertions = augCode.argsExclEndIdxInParentNode
                    }
                }
            }
            const transformSpec: DefaultAstTransformSpec = {
                node: targetNode,
                childIndex: targetIndexForInsertions,
                childToInsert: genCode.node,
                replacementChild: null,
                performDeletion: false
            };
            transformSpecs.push(transformSpec);
        }
    }

    _createGenCodeNode(genCodeLines: string[],
            useInlineMarker: boolean,
            genCodeSection: GeneratedCodeDescriptor | null,
            defaultIndent: string | null,
            defaultLineSep: string | null):  DecoratedLineAstNode | EscapedBlockAstNode {
        if (useInlineMarker) {
            if (genCodeSection && !genCodeSection.nestedBlockUsed) {
                const n = genCodeSection.parentNode.children[genCodeSection.idxInParentNode];
                return AstBuilder.createDecoratedLineNode(
                    getFirst(genCodeLines) || "", n);
            }
            else {
                if (!this.defaultGenCodeInlineMarker) {
                    throw new Error("default gen code inline marker not set");
                }
                const attrs = {
                    indent: defaultIndent,
                    lineSep: defaultLineSep,
                    marker: this.defaultGenCodeInlineMarker
                };
                return AstBuilder.createDecoratedLineNode(
                    getFirst(genCodeLines) || "", attrs);
            }
        }
        else {
            if (genCodeSection && genCodeSection.nestedBlockUsed) {
                const n = genCodeSection.parentNode.children[genCodeSection.idxInParentNode];
                return AstBuilder.createEscapedNode(genCodeLines, n);
            }
            else {
                if (!this.defaultGenCodeStartMarker) {
                    throw new Error("default gen code start marker not set");
                }
                if (!this.defaultGenCodeEndMarker) {
                    throw new Error("default gen code end marker not set");
                }
                const attrs = {
                    indent: defaultIndent,
                    lineSep: defaultLineSep,
                    marker: this.defaultGenCodeStartMarker,
                    endIndent: defaultIndent,
                    endLineSep: defaultLineSep,
                    endMarker: this.defaultGenCodeEndMarker
                };
                return AstBuilder.createEscapedNode(genCodeLines, attrs);
            }
        }
    }

    /**
     * Applies insert/update/delete operations to children of a node.
     * <p>
     * This method requires the following of its transformSpecs arg to
     * guarantee correct operation:
     * <ul>
     * <li>indices per node should be arranged in ascending order.</li>
     * <li>each index to be deleted should not be specified more than once.</li>
     * <li>any inserts should appear towards the end, i.e. once an insert is specified,
     * no other transform type should be specified afterwards.</li>
     * </ul>
     * @param transformSpecs
     */
    static performTransformations(transformSpecs: DefaultAstTransformSpec[]) {
        // since updates don't move indices and need to be applied in order,
        // work on them first.
        for (const transformSpec of transformSpecs) {
            if (transformSpec.performDeletion) {
                continue;
            }
            let nodes = transformSpec.node.children;
            if (transformSpec.replacementChild) {
                nodes[transformSpec.childIndex] = transformSpec.replacementChild;
            }
        }
        // due to nature of deletions and insertions, apply them from last to first.
        for (let i = transformSpecs.length - 1; i >= 0; i--) {
            const transformSpec = transformSpecs[i];
            let nodes = transformSpec.node.children;
            if (transformSpec.performDeletion) {
                nodes.splice(transformSpec.childIndex, 1);
            }
            else if (transformSpec.replacementChild) {
                continue;
            }
            else if (transformSpec.childToInsert) {                
                if (!nodes) {
                    nodes = [];
                    transformSpec.node.children = nodes;
                }
                nodes.splice(transformSpec.childIndex, 0, transformSpec.childToInsert);
            }
        }
    }
}

function findMarkerMatch(markers: string[] | null, marker: any) {
    if (markers) {
        return markers.includes(marker);
    }
    return false;
}

function getLineCount(n: SourceCodeAstNode) {
    if (n.type === AstBuilder.TYPE_DECORATED_LINE ||
            n.type === AstBuilder.TYPE_UNDECORATED_LINE) {
        return 1;
    }
    if (n.type !== AstBuilder.TYPE_ESCAPED_BLOCK &&
            n.type !== AstBuilder.TYPE_NESTED_BLOCK) {
        throw new Error("unexpected node type: " + n.type);
    }
    let count = 2;
    const typedNode = n as (NestedBlockAstNode | EscapedBlockAstNode);
    if (typedNode.children) {
        for (const child of typedNode.children) {
            count += getLineCount(child);
        }
    }
    return count;
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
