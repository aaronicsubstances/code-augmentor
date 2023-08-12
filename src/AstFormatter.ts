import { AstBuilder } from "./AstBuilder";
import {
    DecoratedLineAstNode,
    EscapedBlockAstNode,
    NestedBlockAstNode,
    SourceCodeAst,
    SourceCodeAstNode,
    UndecoratedLineAstNode
} from "./types";

function stringify(n: SourceCodeAstNode) {
    if (!n) {
        throw new Error("received null argument");
    }
    const out = new Array<string>();
    formatAny(n, out);
    return out.join("");
}

function formatAny(n: any, out: string[]) {
    if (n === null || typeof n === "undefined") {
        return;
    }
    switch (n.type) {
        case AstBuilder.TYPE_SOURCE_CODE:
            const typedNode = n as SourceCodeAst;
            if (typedNode.children) {
                typedNode.children.forEach(x => formatAny(x, out));
            }
            break;
        case AstBuilder.TYPE_UNDECORATED_LINE:
            printUndecoratedLine(n as UndecoratedLineAstNode, out);
            break;
        case AstBuilder.TYPE_DECORATED_LINE:
        case AstBuilder.TYPE_NESTED_BLOCK_START:
        case AstBuilder.TYPE_NESTED_BLOCK_END:
        case AstBuilder.TYPE_ESCAPED_BLOCK_START:
        case AstBuilder.TYPE_ESCAPED_BLOCK_END:
            printDecoratedLine(n as DecoratedLineAstNode, out);
            break;
        case AstBuilder.TYPE_ESCAPED_BLOCK:
            printEscapedBlock(n as EscapedBlockAstNode, out);
            break;
        case AstBuilder.TYPE_NESTED_BLOCK:
            printNestedBlock(n as NestedBlockAstNode, out);
            break;
        default:
            throw new Error("unexpected node type: " + n.type);
    }
}

function printUndecoratedLine(n: UndecoratedLineAstNode, out: string[]) {
    out.push(n.text);
    out.push(n.lineSep);
}

/**
 * Also applies to start and end lines of escaped blocks and nested blocks
 * @param n 
 * @param out 
 */
function printDecoratedLine(n: DecoratedLineAstNode, out: string[]) {
    out.push(n.indent);
    out.push(n.marker);
    out.push(n.markerAftermath);
    out.push(n.lineSep);
}

function printEscapedBlock(n: EscapedBlockAstNode, out: string[]) {
    out.push(n.indent);
    out.push(n.marker);
    out.push(n.markerAftermath);
    out.push(n.lineSep);
    if (n.children) {
        // don't mind prescence of non-text children.
        n.children.forEach(x => formatAny(x, out));
    }
    out.push(n.endIndent);
    out.push(n.endMarker);
    // reuse of same marker aftermath.
    out.push(n.markerAftermath);
    out.push(n.endLineSep);
}

function printNestedBlock(n: NestedBlockAstNode, out: string[]) {
    out.push(n.indent);
    out.push(n.marker);
    out.push(n.markerAftermath);
    out.push(n.lineSep);
    if (n.children) {
        n.children.forEach(x => formatAny(x, out));
    }
    out.push(n.endIndent);
    out.push(n.endMarker);
    out.push(n.endMarkerAftermath);
    out.push(n.endLineSep);
}

export const AstFormatter = {
    stringify
}
