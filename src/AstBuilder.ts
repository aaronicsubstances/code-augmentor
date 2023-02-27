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
    _decoratedLinePattern: RegExp | null = null;
    _escapedBlockStartPattern: RegExp | null = null;
    _escapedBlockEndPattern: RegExp | null = null;
    _nestedBlockStartPattern: RegExp | null = null;
    _nestedBlockEndPattern: RegExp | null = null;
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

    static selectSuitableMarkers(markers: string[] | null) {
        if (!markers) {
            return [];
        }
        return markers.filter(AstBuilder.isMarkerSuitable);
    }

    reset() {
        this.decoratedLineMarkers = AstBuilder.selectSuitableMarkers(this.decoratedLineMarkers);
        this._decoratedLinePattern = constructMarkerRegex(this.decoratedLineMarkers);
    
        this.escapedBlockStartMarkers = AstBuilder.selectSuitableMarkers(this.escapedBlockStartMarkers);
        this._escapedBlockStartPattern = constructMarkerRegex(this.escapedBlockStartMarkers);
    
        this.escapedBlockEndMarkers = AstBuilder.selectSuitableMarkers(this.escapedBlockEndMarkers);
        this._escapedBlockEndPattern = constructMarkerRegex(this.escapedBlockEndMarkers);
    
        this.nestedBlockStartMarkers = AstBuilder.selectSuitableMarkers(this.nestedBlockStartMarkers);
        this._nestedBlockStartPattern = constructMarkerRegex(this.nestedBlockStartMarkers);
    
        this.nestedBlockEndMarkers = AstBuilder.selectSuitableMarkers(this.nestedBlockEndMarkers);
        this._nestedBlockEndPattern = constructMarkerRegex(this.nestedBlockEndMarkers);
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
        if (findRegexMatch(this._nestedBlockEndPattern, n.text.substring(
                n.indent.length))) {
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
        let valueMinusIndent = n.text.substring(n.indent.length);
        let m = findRegexMatch(this._nestedBlockStartPattern, valueMinusIndent);
        if (!m) {
            return null;
        }
        this._consumeAsDecoratedLine();
        const parentNodeLineNum = this._peekIdx;
        const parent = n as NestedBlockAstNode;
        parent.type = AstBuilder.TYPE_NESTED_BLOCK;
        parent.marker = m[1];
        parent.markerAftermath = valueMinusIndent.substring(m[0].length);
        parent.children = [];
        while (n = this._peek()) {
            valueMinusIndent = n.text.substring(n.indent.length);
            m = findRegexMatch(this._nestedBlockEndPattern, valueMinusIndent);
            if (m) {
                this._consumeAsDecoratedLine();
                parent.endIndent = n.indent;
                parent.endLineSep = n.lineSep;
                parent.endMarker = m[1];
                parent.endMarkerAftermath = valueMinusIndent.substring(m[0].length);
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
        let valueMinusIndent = n.text.substring(n.indent.length);
        let m = findRegexMatch(this._escapedBlockStartPattern, valueMinusIndent);
        if (!m) {
            return null;
        }
        this._consumeAsDecoratedLine();
        const parentNodeLineNum = this._peekIdx;
        const parent = n as EscapedBlockAstNode;
        parent.type = AstBuilder.TYPE_ESCAPED_BLOCK;
        parent.marker = m[1];
        parent.markerAftermath = valueMinusIndent.substring(m[0].length);
        parent.children = [];
        while (n = this._peek()) {
            valueMinusIndent = n.text.substring(n.indent.length);
            m = findRegexMatch(this._escapedBlockEndPattern, valueMinusIndent);
            if (m && valueMinusIndent.substring(m[0].length) === parent.markerAftermath) {
                this._consumeAsDecoratedLine();
                parent.endIndent = n.indent;
                parent.endLineSep = n.lineSep;
                parent.endMarker = m[1];
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
        const valueMinusIndent = n.text.substring(n.indent.length);
        const m = findRegexMatch(this._decoratedLinePattern, valueMinusIndent);
        if (!m) {
            return null;
        }
        this._consumeAsDecoratedLine();
        const typedNode = n as DecoratedLineAstNode;
        typedNode.type = AstBuilder.TYPE_DECORATED_LINE;
        typedNode.marker = m[1];
        typedNode.markerAftermath = valueMinusIndent.substring(m[0].length);
        return typedNode;
    }
}

function constructMarkerRegex(markers: string[] | null) {
    if (!markers || !markers.length) {
        return null;
    }
    // escape markers.
    markers = markers.map(m => m.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    // let longer strings appear first to ensure if a marker is a prefix
    // of another, the latter and longer one is chosen.
    markers.sort((x, y) => y.length - x.length);
    const lexerRegexBuilder = markers.join("|");
    return new RegExp("^(" + lexerRegexBuilder + ")");
}

function findRegexMatch(p: RegExp | null, s: string) {
    if (p) {
        return p.exec(s);
    }
    return null;
}
