import AstBuilder from "./AstBuilder";
import {
    DecoratedLineAstNode,
    EscapedBlockAstNode,
    NestedBlockAstNode,
    SourceCodeAst,
    UndecoratedLineAstNode
} from "./types";

function stringify(n: any) {
    const out = new Array<string>();
    if (n.type === AstBuilder.TYPE_SOURCE_CODE) {
        const typedNode = n as SourceCodeAst;
        if (typedNode.children) {
            typedNode.children.forEach(x => formatAny(x, out));
        }
    }
    else {
        formatAny(n, out);
    }
    // leverage behaviour of Array.join to skip null and undefined elements,
    // so that at runtime null and undefined members of nodes don't cause
    // problems.
    return out.join("");
}

function formatAny(n: any, out: string[]) {
    switch (n.type) {
        case AstBuilder.TYPE_UNDECORATED_LINE:
            printUndecoratedLine(n as UndecoratedLineAstNode, out);
            break;
        case AstBuilder.TYPE_DECORATED_LINE:
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

export default {
    stringify
}
