export interface SourceCodeAst {
    type: number;
    children: SourceCodeAstNode[];
}

export interface UndecoratedLineAstNode {
    type: number;
    text: string;
    lineSep: string;
}

export interface DecoratedLineAstNode {
    type: number;
    indent: string;
    marker: string;
    markerAftermath: string;
    lineSep: string;
}

export interface EscapedBlockAstNode extends DecoratedLineAstNode {
    endIndent: string;
    endMarker: string;
    endLineSep: string;
    children: UndecoratedLineAstNode[];
}

export interface NestedBlockAstNode extends Omit<EscapedBlockAstNode, "children"> {
    endMarkerAftermath: string;
    children: SourceCodeAstNode[];
}

export type SourceCodeAstNode =
    | UndecoratedLineAstNode
    | DecoratedLineAstNode
    | EscapedBlockAstNode
    | NestedBlockAstNode;

export interface AugmentingCodeDescriptor {
    parentNode: SourceCodeAst | NestedBlockAstNode;
    idxInParentNode: number;
    nestedBlockUsed: boolean;
    lineNumber: number;
    functionName: string;
    args: any[];
    argsExclEndIdxInParentNode: number;
    endFunctionName: string | null;
    endArgs: any[] | null;
    endArgsExclEndIdxInParentNode: number;
    parent: AugmentingCodeDescriptor | null;
    children: AugmentingCodeDescriptor[];
}

export interface GeneratedCodeDescriptor {
    parentNode: SourceCodeAst | NestedBlockAstNode;
    idxInParentNode: number;
    nestedBlockUsed: boolean;
}

export interface GeneratedCodePart {
    content: string | null;
    exempt: boolean;
}

export interface GeneratedCode {
    contentParts: GeneratedCodePart[] | null;
    indent: string | null;
    useInlineMarker: boolean;
    ignore: boolean;
    ignoreRemainder: boolean;
}

export interface GeneratedCodeSectionTransform {
    node: SourceCodeAstNode | null;
    ignore: boolean;
    ignoreRemainder: boolean;
}

export interface DefaultAstTransformSpec {
    node: SourceCodeAst | EscapedBlockAstNode | NestedBlockAstNode;
    childIndex: number;
    childToInsert: SourceCodeAstNode | null;
    replacementChild: SourceCodeAstNode | null;
    performDeletion: boolean;
}

export interface SourceFileLocation {
    baseDir: string;
    relativePath: string;
}

export interface SourceFileDescriptor {
    baseDir: string | null;
    relativePath: string;
    content: string;
    encoding: BufferEncoding | null;
    binaryContent: Buffer | null;
}
