import * as utils from "./utils";
import {
    SourceCodeAst,
    DecoratedLineAstNode,
    EscapedBlockAstNode,
    NestedBlockAstNode
} from "./types";

export default class AstBuilder {
    decoratedLineMarkers: string[] | null = null;
    escapedBlockStartMarkers: string[] | null = null;
    escapedBlockEndMarkers: string[] | null = null;
    nestedBlockStartMarkers :string[] | null = null;
    nestedBlockEndMarkers: string[] | null = null;
    _nodes = new Array<any>();
    _peekIdx = -1;
    _srcPath: string | null = null;

    static TYPE_SOURCE_CODE = 1;
    static TYPE_UNDECORATED_LINE = 2;
    static TYPE_DECORATED_LINE = 3;
    static TYPE_ESCAPED_BLOCK = 4;
    static TYPE_NESTED_BLOCK = 5;

    static isMarkerSuitable(marker: string | null) {
        return marker && !utils.determineIndent(marker);
    }

    static _findMarkerMatch(markers: string[] | null, n: any) {
        if (!markers) {
            return null;
        }
        let latestFind = '';
        for (const marker of markers) {
            if (!marker) {
                continue;
            }
            // don't bother checking if indent+marker exceeds text bounds.
            if (n.indent.length + marker.length > n.text.length) {
                continue;
            }
            let matchFound = true;
            for (let i = 0; i < marker.length; i++) {
                const a = marker[i];
                const b = n.text[n.indent.length + i];
                if (a !== b) {
                    matchFound = false;
                    break;
                }
            }
            if (matchFound) {
                // pick the longest match, and if multiple candidates are found.
                // pick the earliest of them.
                if (marker.length > latestFind.length) {
                    latestFind = marker;
                }
            }
        }
        if (!latestFind) {
            return null;
        }
        return new Array<string>(latestFind, n.text.substring(n.indent.length + latestFind.length));
    }
    

    parse(source: string, srcPath: string) {
        // reset.
        this._srcPath = srcPath;
        this._nodes = [];
        this._peekIdx = 0;
    
        const splitSource = utils.splitIntoLines(source, true);
        for (let i = 0; i < splitSource.length; i+=2) {
            const line = splitSource[i];
            const terminator = splitSource[i + 1];
            const indent = utils.determineIndent(line);
            const n = {
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: line,
                lineSep: terminator,
                indent
            };
            this._nodes.push(n);
        }
        const root: SourceCodeAst = {
            type: AstBuilder.TYPE_SOURCE_CODE,
            children: []
        };
        let child;
        while (child = this._matchAny()) {
            root.children.push(child);
        }

        return root;
    }

    _peek() {
        if (this._peekIdx < this._nodes.length) {
            return this._nodes[this._peekIdx];
        }
        return null;
    }
    
    _consumeAsDecoratedLine() {
        const n = this._nodes[this._peekIdx++];
        delete n.text;
        return n;
    }
    
    _consumeAsUndecoratedLine() {
        const n = this._nodes[this._peekIdx++];
        delete n.indent;
        return n;
    }
    
    _abort(lineNum: number, msg: string) {
        let srcPathDesc = "";
        if (this._srcPath) {
            srcPathDesc = "in " + this._srcPath + " ";
        }
        const lineNumDesc = "at llne " + lineNum + " ";
        throw new Error(srcPathDesc + lineNumDesc + msg);
    }
    
    _matchAny() {
        let n = this._peek();
        if (!n) {
            return null;
        }
        if (AstBuilder._findMarkerMatch(this.nestedBlockEndMarkers, n)) {
            throw this._abort(this._peekIdx + 1, "encountered complex end tag without " +
                "matching start tag");
        }
        n = this._matchNestedBlock();
        if (!n) {
            n = this._matchEscapedBlock();
        }
        if (!n ) {
            n = this._matchDecoratedLine();
        }
        if (!n) {
            n = this._consumeAsUndecoratedLine();
        }
        return n;
    }

    _matchNestedBlock() {
        let n = this._peek();
        let m = AstBuilder._findMarkerMatch(this.nestedBlockStartMarkers, n);
        if (!m) {
            return null;
        }
        this._consumeAsDecoratedLine();
        const parentNodeLineNum = this._peekIdx;
        const parent = n as NestedBlockAstNode;
        parent.type = AstBuilder.TYPE_NESTED_BLOCK;
        parent.marker = m[0];
        parent.markerAftermath = m[1];
        parent.children = [];
        while (n = this._peek()) {
            m = AstBuilder._findMarkerMatch(this.nestedBlockEndMarkers, n);
            if (m) {
                this._consumeAsDecoratedLine();
                parent.endIndent = n.indent;
                parent.endLineSep = n.lineSep;
                parent.endMarker = m[0];
                parent.endMarkerAftermath = m[1];
                break;
            }
            else {
                parent.children.push(this._matchAny());
            }
        }
        if (!n) {
            throw this._abort(parentNodeLineNum, "matching nested block ending line not found");
        }
        return parent;
    }

    _matchEscapedBlock() {
        let n = this._peek();
        let m = AstBuilder._findMarkerMatch(this.escapedBlockStartMarkers, n);
        if (!m) {
            return null;
        }
        this._consumeAsDecoratedLine();
        const parentNodeLineNum = this._peekIdx;
        const parent = n as EscapedBlockAstNode;
        parent.type = AstBuilder.TYPE_ESCAPED_BLOCK;
        parent.marker = m[0];
        parent.markerAftermath = m[0];
        parent.children = [];
        while (n = this._peek()) {
            m = AstBuilder._findMarkerMatch(this.escapedBlockEndMarkers, n);
            if (m && m[1] === parent.markerAftermath) {
                this._consumeAsDecoratedLine();
                parent.endIndent = n.indent;
                parent.endLineSep = n.lineSep;
                parent.endMarker = m[0];
                break;
            }
            else {
                this._consumeAsUndecoratedLine();
                parent.children.push(n);
            }
        }
        if (!n) {
            throw this._abort(parentNodeLineNum, "matching escaped block ending line not found");
        }
        return parent;
    }    

    _matchDecoratedLine() {
        const n = this._peek();
        const m = AstBuilder._findMarkerMatch(this.decoratedLineMarkers, n);
        if (!m) {
            return null;
        }
        this._consumeAsDecoratedLine();
        const typedNode = n as DecoratedLineAstNode;
        typedNode.type = AstBuilder.TYPE_DECORATED_LINE;
        typedNode.marker = m[0];
        typedNode.markerAftermath = m[1];
        return typedNode;
    }
}
