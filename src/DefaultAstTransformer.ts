import os from "os";

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

    _addAugCodes(parentNode: SourceCodeAst | NestedBlockAstNode,
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
                    functionName: typedNode.markerAftermath,
                    args: augCodeArgs.args,
                    argsExclEndIdxInParentNode: augCodeArgs.exclEndIdx,
                    endFunctionName: typedNode.endMarkerAftermath,
                    endArgs: augCodeEndArgs.args,
                    endArgsExclEndIdxInParentNode: augCodeEndArgs.exclEndIdx,
                    index: dest.length,
                    parentIndex: -1,
                    childIndices: []
                };
                dest.push(augCode);
                if (parentAugCode) {
                    augCode.parentIndex = parentAugCode.index;
                    parentAugCode.childIndices.push(augCode.index);
                }
                this._addAugCodes(typedNode, augCode, lineCounter, dest);
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
                    functionName: typedNode.markerAftermath,
                    args: augCodeArgs.args,
                    argsExclEndIdxInParentNode: augCodeArgs.exclEndIdx,
                    endFunctionName: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    index: dest.length,
                    parentIndex: -1,
                    childIndices: []
                };
                dest.push(augCode);
                if (parentAugCode) {
                    augCode.parentIndex = parentAugCode.index;
                    parentAugCode.childIndices.push(augCode.index);
                }
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
                exclEndIdx: -1,
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
            if (findMarkerMatch(this.augCodeJsonArgMarkers, typedNode.marker)) {
                args.push(typedNode.markerAftermath);
                args.push(typedNode.lineSep);
                args.push(true);
            }
            else if (findMarkerMatch(this.augCodeArgMarkers, typedNode.marker)) {
                args.push(typedNode.markerAftermath);
                args.push(typedNode.lineSep);
                args.push(false);
            }
            else if (findMarkerMatch(this.augCodeArgSepMarkers, typedNode.marker)) {
                args.push(null);
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

    applyGeneratedCodes(augCodes: AugmentingCodeDescriptor[],
            augCodeIndex: number,
            genCodes: GeneratedCode[]) {
        const augCode = augCodes[augCodeIndex];
        const augCodeNode = augCode.parentNode.children[augCode.idxInParentNode] as 
            (NestedBlockAstNode | DecoratedLineAstNode);
        const genCodeTransforms = new Array<GeneratedCodeSectionTransform | null>();
        const genCodeSections = this.extractGenCodeSections(augCodes, augCodeIndex);

        // begin by processing all but the last gen code.
        let genCodeSectionReduction = 0;
        if (augCode.nestedBlockUsed) {
            const lastGenCodeSection = genCodeSections.at(-1);
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
        const augCodeTransforms = this.computeAugCodeTransforms(augCodes, augCodeIndex,
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
                const node = this.createGenCodeNode(genCodeLines,
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
                const node = this.createGenCodeNode(genCodeLines, genCode.useInlineMarker,
                    null, defaultIndent, defaultLineSep);
                transform.node = node;
            }
            genCodeTransforms.push(transform);
        }
    }

    extractGenCodeSections(augCodes: AugmentingCodeDescriptor[], augCodeIndex: number) {
        const genCodeSections = new Array<GeneratedCodeDescriptor>();
        const augCode = augCodes[augCodeIndex];
        if (augCode.nestedBlockUsed) {
            this._addNestedGenCodeSections(augCodes, augCodeIndex, genCodeSections);
        }
        const lastGenCodeSection = this._getLastGenCodeSection(augCode);
        if (lastGenCodeSection) {
            genCodeSections.push(lastGenCodeSection);
        } 
        return genCodeSections;
    }

    _addNestedGenCodeSections(augCodes: AugmentingCodeDescriptor[],
            augCodeIndex: number,
            genCodeSections: GeneratedCodeDescriptor[]) {
        // get all gen code nodes except those functioning as
        // last gen code sections for child aug codes.
        const augCode = augCodes[augCodeIndex];
        const exemptions = augCode.childIndices
            .map(aIdx => {
                const g = this._getLastGenCodeSection(augCodes[aIdx]);
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
        DefaultAstTransformer.repairSplitCrLfs(contentParts);

        const allLines = new Array<string>();
        let lastPartIsExemptAndEmpty = false;
        let lastPartEndedWithLineSep = true;
        for (const code of contentParts) {
            if (!code) {
                continue;
            }
            if (!code.content) {
                if (code.exempt) {
                    lastPartIsExemptAndEmpty = true;
                }
                else {
                    // pass through value of previous lastPartEndedWithLineSep
                }
                continue;
            }
            const splitCode = myutils.splitIntoLines(code.content, true);
            for (let j = 0; j < splitCode.length; j+=2) {
                const line = splitCode[j];
                let terminator = splitCode[j + 1];
                if (!(!lineSeparator || code.exempt)) {
                    terminator = lineSeparator;
                }
                if (j > 0 || lastPartEndedWithLineSep) {
                    // determine indent to apply.
                    // as a policy don't indent blank lines, similar
                    // to what IDEs do.
                    let effectiveIndent = "";
                    if (!(!indent || code.exempt || lastPartIsExemptAndEmpty ||
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
                        throw new Error("algorithm failed. expected empty terminator");
                    }
                    allLines[allLines.length - 2] += line;
                    allLines[allLines.length - 1] = terminator;
                }
            }
            lastPartIsExemptAndEmpty = false;
            lastPartEndedWithLineSep = !!allLines[allLines.length - 1];
        }

        // differentiate between empty content parts, and
        // content parts with empty contents.
        if (contentParts.length > 0) {
            if (allLines.length === 0) {
                allLines.push("");
                allLines.push(lineSeparator || "");
            }

            // ensure ending terminator, but only if line separator was given,
            // so as to provide a way to skip appending of ending newlines.
            if (lineSeparator && !allLines[allLines.length - 1]) {
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
    static repairSplitCrLfs(contentParts: GeneratedCodePart[]) {
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
            augCodes: AugmentingCodeDescriptor[],
            augCodeIndex: number,
            genCodes: Array<GeneratedCodeSectionTransform | null>,
            genCodeSections: GeneratedCodeDescriptor[] | null) {
        const augCode = augCodes[augCodeIndex];
        const augCodeNode = augCode.parentNode.children[augCode.idxInParentNode];
        if (!genCodeSections) {
            genCodeSections = this.extractGenCodeSections(augCodes, augCodeIndex);
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
            const g = genCodes.at(-1);
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
                const g = genCodeSections.at(-1);
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

    createGenCodeNode(genCodeLines: string[],
            useInlineMarker: boolean,
            genCodeSection: GeneratedCodeDescriptor | null,
            defaultIndent: string | null,
            defaultLineSep: string | null) {
        if (useInlineMarker) {
            if (genCodeSection && !genCodeSection.nestedBlockUsed) {
                const n = genCodeSection.parentNode.children[genCodeSection.idxInParentNode];
                return DefaultAstTransformer.createDecoratedLineNode(
                    genCodeLines.at(0) || "", n);
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
                return DefaultAstTransformer.createDecoratedLineNode(
                    genCodeLines.at(0) || "", attrs);
            }
        }
        else {
            if (genCodeSection && genCodeSection.nestedBlockUsed) {
                const n = genCodeSection.parentNode.children[genCodeSection.idxInParentNode];
                return DefaultAstTransformer.createEscapedNode(genCodeLines, n);
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
                return DefaultAstTransformer.createEscapedNode(genCodeLines, attrs);
            }
        }
    }

    static createDecoratedLineNode(line: string, attrs: any) {
        if (!attrs) {
            attrs = {};
        }
        if (!AstBuilder.isMarkerSuitable(attrs.marker)) {
            throw new Error("received unsuitable marker: " + attrs.marker);
        }
        const n: any = {
            type: AstBuilder.TYPE_DECORATED_LINE,
            marker: attrs.marker,
            markerAftermath: line,
            indent: attrs.indent,
            lineSep: attrs.lineSep
        }

        // supply defaults for unset props.
        if (!n.indent) {
            n.indent = "";
        }
        if (!n.lineSep) {
            n.lineSep = os.EOL;
        }

        return n as DecoratedLineAstNode;
    }

    static createEscapedNode(lines: string[], attrs: any) {
        if (!attrs) {
            attrs = {};
        }
        if (!AstBuilder.isMarkerSuitable(attrs.marker)) {
            throw new Error("received unsuitable start marker: " + attrs.marker);
        }
        if (!AstBuilder.isMarkerSuitable(attrs.endMarker)) {
            throw new Error("received unsuitable end marker: " + attrs.endMarker);
        }
        let markerAftermath = attrs.markerAftermath || "";
        const uniqueEndMarkerPlus = myutils.modifyNameToBeAbsent(
            lines, attrs.endMarker + markerAftermath);
        markerAftermath = uniqueEndMarkerPlus.substring(attrs.endMarker.length);
        
        const n: any = {
            type: AstBuilder.TYPE_ESCAPED_BLOCK,
            marker: attrs.marker,
            endMarker: attrs.endMarker,
            markerAftermath: attrs.markerAftermath,
            indent: attrs.indent,
            endIndent: attrs.endIndent,
            lineSep: attrs.lineSep,
            endLineSep: attrs.endLineSep,
            children: []
        };

        // supply defaults for unset props.
        if (!n.indent) {
            n.indent = "";
        }
        if (!n.endIndent) {
            n.endIndent = "";
        }
        if (!n.lineSep) {
            n.lineSep = os.EOL;
        }
        if (!n.endLineSep) {
            n.endLineSep = os.EOL;
        }

        for (let i = 0; i < lines.length; i+=2) {
            const line = lines[i];
            const terminator = lines[i + 1];

            n.children.push({
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: line,
                lineSep: terminator || os.EOL
            });
        }

        return n as EscapedBlockAstNode;
    }

    /**
     * Applies insert/update/delete operations to children of a node.
     * <p>
     * This method requires the following of its transformSpecs arg to
     * guarantee correct operation:
     * <ul>
     * <li>indices per node (ie parent or child) should be arranged in ascending order.</li>
     * <li>each index to be deleted should not be specified more than once.</li>
     * <li>any inserts should appear towards the end, i.e. once an insert is specified,
     * no other transform type should be specified afterwards.</li>
     * </ul>
     * @param transformSpecs
     */
    static performTransformations(transformSpecs: DefaultAstTransformSpec[]) {
        // due to nature of deletions and insertions shifting indices,
        // transform from last to first.
        for (let i = transformSpecs.length - 1; i >= 0; i--) {
            const transformSpec = transformSpecs[i];
            let nodes = transformSpec.node.children;
            if (!nodes) {
                nodes = [];
                transformSpec.node.children = nodes;
            }
            if (transformSpec.performDeletion) {
                nodes.splice(transformSpec.childIndex, 1);
            }
            else if (transformSpec.replacementChild) {
                nodes[transformSpec.childIndex] = transformSpec.replacementChild;
            }
            else if (transformSpec.childToInsert) {
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
    if (n.type === AstBuilder.TYPE_ESCAPED_BLOCK) {
        const typedNode = n as EscapedBlockAstNode;
        return 2 + (typedNode.children ? typedNode.children.length : 0);
    }
    if (n.type === AstBuilder.TYPE_NESTED_BLOCK) {
        let count = 2;
        const typedNode = n as NestedBlockAstNode;
        if (typedNode.children) {
            for (const child of typedNode.children) {
                count += getLineCount(child);
            }
        }
        return count;
    }
    throw new Error("unexpected node type: " + n.type);
}
