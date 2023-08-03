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
    markerAftermath: string;
    args: any[];
    argsExclEndIdxInParentNode: number;
    endMarkerAftermath: string | null;
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
    appendChangeDetails(data: string): Promise<void>;
    normalizeSrcFileLoc(srcFileDescriptor: SourceFileDescriptor): SourceFileLocation;
    stringifySrcFileLoc(loc: SourceFileLocation): string;
    stringifyDestFileLoc(loc: SourceFileLocation): string;
    areFileContentsEqual(arg1: string | Buffer, arg2: string | Buffer, isBinary: boolean): boolean;
    generateDestFileLoc(srcFileLoc: SourceFileLocation): SourceFileLocation;
}

export interface CodeChangeDetectiveConfigFactory {
    create(): Promise<CodeChangeDetectiveConfig>;
}