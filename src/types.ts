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

export interface SourceFileLocation {
    baseDir?: string | null;
    relativePath?: string | null;
}
