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
    | NestedBlockAstNode
    | SourceCodeAst;

export interface AugmentingCodeDescriptor2 { // remove
    parentNode: SourceCodeAst | NestedBlockAstNode;
    idxInParentNode: number;
    nestedBlockUsed: boolean;
    lineNumber: number;
    markerAftermath: string;
    args: any[];
    argsExclEndIdxInParentNode: number;
    endMarkerAftermath: string | null;
    endArgs: any[] | null;
    endArgsExclEndIdxInParentNode: number;
    parent: AugmentingCodeDescriptor2 | null;
    children: AugmentingCodeDescriptor2[];
}

export interface GeneratedCodeDescriptor { // remove
    parentNode: SourceCodeAst | NestedBlockAstNode;
    idxInParentNode: number;
    nestedBlockUsed: boolean;
}

export interface AugmentingCodeDescriptor {
    startIdx: number;
    exclEndIdx: number;
    lineObj: LineObj;
    node: DecoratedLineAstNode;
    args: any[];
}

export interface LineObj {
    node: SourceCodeAstNode;
    lineNumber: number;
    type: number;
    arg?: any;
    updates?: SourceCodeAstNode[];
}

export interface GeneratedCodePart {
    content: string | null;
    exempt: boolean;
}

export interface GeneratedCode {
    contentParts: GeneratedCodePart[] | null;
    indent: string | null;
    useInlineMarker: boolean; // remove.
    ignore: boolean;
    ignoreRemainder: boolean;
    lineSep?: string;
    markerType?: number;
}

export interface GeneratedCodeSectionTransform { // internal
    ignore?: boolean; // remove
    node: SourceCodeAstNode | null; // null means ignore
    ignoreRemainder: boolean;
}

export interface DefaultAstTransformSpec { // remove
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
    baseDir?: string;
    relativePath: string;
    content?: string;
    encoding?: BufferEncoding;
    binaryContent?: Buffer;
    isBinary: boolean;
}

export interface CodeChangeDetectiveConfig {
    release(): Promise<void>;
    getFileContent(loc: SourceFileLocation, isBinary: boolean,
        encoding?: BufferEncoding): Promise<string | Buffer>;
    saveFileContent(loc: SourceFileLocation, data: string | Buffer,
        isBinary: boolean, encoding?: BufferEncoding): Promise<void>;
    appendOutputSummary(data: string): Promise<void>;
    appendChangeSummary(data: string): Promise<void>;
    appendChangeDiff(data: string): Promise<void>;
    normalizeSrcFileLoc(srcFileDescriptor: SourceFileDescriptor): SourceFileLocation;
    stringifySrcFileLoc(loc: SourceFileLocation): string;
    stringifyDestFileLoc(loc: SourceFileLocation): string;
    areFileContentsEqual(arg1: string | Buffer, arg2: string | Buffer, isBinary: boolean): boolean;
    generateDestFileLoc(srcFileLoc: SourceFileLocation): SourceFileLocation;
}

export interface CodeChangeDetectiveConfigFactory {
    create(): Promise<CodeChangeDetectiveConfig>;
}