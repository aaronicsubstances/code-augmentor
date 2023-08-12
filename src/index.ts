export * from "./AstBuilder";
export * from "./AstFormatter";
export * from "./CodeChangeDetective";
export * from "./DefaultAstTransformer";
export * from "./DefaultCodeGenerationStrategy";
export * from "./types";

import * as myutils from "./helperUtils";
export const helperUtils = {
    splitIntoLines: myutils.splitIntoLines,
    isBlank: myutils.isBlank,
    cleanDir: myutils.cleanDir,
    determineIndent: myutils.determineIndent,
    modifyNameToBeAbsent: myutils.modifyNameToBeAbsent,
    modifyTextToBeAbsent: myutils.modifyTextToBeAbsent,
    normalizeSrcFileLoc: myutils.normalizeSrcFileLoc,
    printNormalDiff: myutils.printNormalDiff
};