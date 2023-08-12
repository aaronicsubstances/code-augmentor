import os from "os";

import * as myutils from "./helperUtils";
import {
    SourceCodeAst,
    DecoratedLineAstNode,
    EscapedBlockAstNode,
    NestedBlockAstNode,
    SourceCodeAstNode,
    UndecoratedLineAstNode
} from "./types";

const MARKER_SUITABILITY_REGEX = new RegExp(/^\s|\r|\n/);
const UNNEEDED_MATCH_DETAILS_RESULT = new Array<string>();

export class AstBuilder {
    decoratedLineMarkers: string[] | null = null;
    escapedBlockStartMarkers: string[] | null = null;
    escapedBlockEndMarkers: string[] | null = null;
    nestedBlockStartMarkers :string[] | null = null;
    nestedBlockEndMarkers: string[] | null = null;
    private _nodes = new Array<any>();
    private _peekIdx = -1;
    private _srcPath: string | null = null;

    static TYPE_SOURCE_CODE = 1;
    static TYPE_UNDECORATED_LINE = 2;
    static TYPE_DECORATED_LINE = 3;
    static TYPE_ESCAPED_BLOCK = 4;
    static TYPE_NESTED_BLOCK = 5;
    static TYPE_ESCAPED_BLOCK_START = 6;
    static TYPE_ESCAPED_BLOCK_END = 7;
    static TYPE_NESTED_BLOCK_START = 8;
    static TYPE_NESTED_BLOCK_END = 9;

    static isMarkerSuitable(marker: string | null) {
        return marker && !MARKER_SUITABILITY_REGEX.exec(marker);
    }

    static _findMarkerMatch(markers: string[] | null, n: any, matchDetailsNotNeeded = false) {
        if (!markers) {
            return null;
        }
        // if match details are needed, then
        // pick the longest match, and if multiple candidates are found.
        // pick the earliest of them.
        let latestFind = '';
        for (const marker of markers) {
            if (!marker || marker.length <= latestFind.length) {
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
                latestFind = marker;
                if (matchDetailsNotNeeded) {
                    break;
                }
            }
        }
        if (!latestFind) {
            return null;
        }
        else if (matchDetailsNotNeeded) {
            return UNNEEDED_MATCH_DETAILS_RESULT;
        }
        return new Array<string>(latestFind, n.text.substring(n.indent.length + latestFind.length));
    }

    parse(source: string, srcPath: string | null = null) {
        // reset.
        this._srcPath = srcPath;
        this._nodes = [];
        this._peekIdx = 0;

        const splitSource = myutils.splitIntoLines(source, true);
        for (let i = 0; i < splitSource.length; i+=2) {
            const line = splitSource[i];
            const terminator = splitSource[i + 1];
            const indent = myutils.determineIndent(line);
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

    private _peek() {
        if (this._peekIdx < this._nodes.length) {
            return this._nodes[this._peekIdx];
        }
        return null;
    }

    private _consumeAsDecoratedLine() {
        const n = this._nodes[this._peekIdx++];
        delete n.text;
        return n;
    }

    private _consumeAsUndecoratedLine() {
        const n = this._nodes[this._peekIdx++];
        delete n.indent;
        return n;
    }

    private _abort(lineNum: number, msg: string) {
        let srcPathDesc = "";
        if (this._srcPath) {
            srcPathDesc = "in " + this._srcPath + " ";
        }
        const lineNumDesc = "at llne " + lineNum + " ";
        throw new Error(srcPathDesc + lineNumDesc + msg);
    }

    private _matchAny() {
        let n = this._peek();
        if (!n) {
            return null;
        }
        if (AstBuilder._findMarkerMatch(this.nestedBlockEndMarkers, n)) {
            throw this._abort(this._peekIdx + 1, "encountered nested block end line without " +
                "matching start line");
        }
        if (AstBuilder._findMarkerMatch(this.escapedBlockEndMarkers, n)) {
            throw this._abort(this._peekIdx + 1, "encountered escaped block end line without " +
                "matching start line");
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

    private _matchNestedBlock() {
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
                if (!n.lineSep) {
                    throw new Error("any line other than undecorated lines must end with a line separator");
                }
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
            throw this._abort(parentNodeLineNum, "matching nested block end line not found");
        }
        return parent;
    }

    private _matchEscapedBlock() {
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
        parent.markerAftermath = m[1];
        parent.children = [];
        while (n = this._peek()) {
            m = AstBuilder._findMarkerMatch(this.escapedBlockEndMarkers, n);
            if (m && m[1] === parent.markerAftermath) {
                if (!n.lineSep) {
                    throw new Error("any line other than undecorated lines must end with a line separator");
                }
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
            throw this._abort(parentNodeLineNum, "matching escaped block end line not found");
        }
        return parent;
    }

    private _matchDecoratedLine() {
        const n = this._peek();
        const m = AstBuilder._findMarkerMatch(this.decoratedLineMarkers, n);
        if (!m) {
            return null;
        }
        if (!n.lineSep) {
            throw new Error("any line other than undecorated lines must end with a line separator");
        }
        this._consumeAsDecoratedLine();
        const typedNode = n as DecoratedLineAstNode;
        typedNode.type = AstBuilder.TYPE_DECORATED_LINE;
        typedNode.marker = m[0];
        typedNode.markerAftermath = m[1];
        return typedNode;
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

        // validate indent and lineSep
        if (!myutils.isBlank(n.indent)) {
            throw new Error("received non-blank indent: " + n.indent);
        }
        if (n.lineSep !== '\r' && n.lineSep !== '\n' && n.lineSep !== '\r\n') {
            throw new Error("received invalid lineSep: " + n.lineSep);
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
        const uniqueEndMarkerPlus = myutils.modifyTextToBeAbsent(
            lines, attrs.endMarker + markerAftermath);
        markerAftermath = uniqueEndMarkerPlus.substring(attrs.endMarker.length);

        const n: any = {
            type: AstBuilder.TYPE_ESCAPED_BLOCK,
            marker: attrs.marker,
            endMarker: attrs.endMarker,
            markerAftermath,
            indent: attrs.indent,
            endIndent: attrs.endIndent,
            lineSep: attrs.lineSep,
            endLineSep: attrs.endLineSep,
            children: []
        };

        // validate indents and lineSeps
        if (!myutils.isBlank(n.indent)) {
            throw new Error("received non-blank indent: " + n.indent);
        }
        if (!myutils.isBlank(n.endIndent)) {
            throw new Error("received non-blank end indent: " + n.endIndent);
        }
        if (n.lineSep !== '\r' && n.lineSep !== '\n' && n.lineSep !== '\r\n') {
            throw new Error("received invalid lineSep: " + n.lineSep);
        }
        if (n.endLineSep !== '\r' && n.endLineSep !== '\n' && n.endLineSep !== '\r\n') {
            throw new Error("received invalid end lineSep: " + n.endLineSep);
        }

        for (let i = 0; i < lines.length; i+=2) {
            const line = lines[i];
            const terminator = lines[i + 1];

            n.children.push({
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: line,
                lineSep: terminator || n.lineSep
            });
        }

        return n as EscapedBlockAstNode;
    }
}
