import { SourceCodeAstNode } from "./types";

export interface GeneratedCodeSectionTransform { // internal
    node: SourceCodeAstNode | null; // null means ignore
    ignoreRemainder: boolean;
}