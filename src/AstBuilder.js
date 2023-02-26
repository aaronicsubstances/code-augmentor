const utils = require("./utils");

// Class constructor.
function AstBuilder() {
    this.simpleTagMarkers = [];
    this.escapedBlockStartMarkers = [];
    this.escapedBlockEndMarkers = [];
    this.complexTagStartMarkers = [];
    this.complexTagEndMarkers = [];
    this._simpleTagPattern = null;
    this._escapedBlockStartPattern = null;
    this._escapedBlockEndPattern = null;
    this._complexStartTagPattern = null;
    this._complexEndTagPattern = null;
    this._nodes = [];
    this._peekIdx = 0;
    this._srcPath = "";
}

AstBuilder.TYPE_TEXT_NODE = 1;

AstBuilder.TYPE_SIMPLE_TAG = 2;

AstBuilder.TYPE_ESCAPED_BLOCK = 3;

AstBuilder.TYPE_COMPLEX_TAG = 4;

AstBuilder.isMarkerSuitable = function(marker) {
    return marker && !utils.determineIndent(marker);
}

AstBuilder.selectSuitableMarkers = function(markers) {
    if (!markers) {
        return [];
    }
    return markers.filter(AstBuilder.isMarkerSuitable);
}

function constructMarkerRegex(markers) {
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

function findRegexMatch(p, s) {
    if (p) {
        return p.exec(s);
    }
    return null;
}

AstBuilder.prototype.reset = function() {
    this.simpleTagMarkers = AstBuilder.selectSuitableMarkers(this.simpleTagMarkers);
    this._simpleTagPattern = constructMarkerRegex(this.simpleTagMarkers);

    this.escapedBlockStartMarkers = AstBuilder.selectSuitableMarkers(this.escapedBlockStartMarkers);
    this._escapedBlockStartPattern = constructMarkerRegex(this.escapedBlockStartMarkers);

    this.escapedBlockEndMarkers = AstBuilder.selectSuitableMarkers(this.escapedBlockEndMarkers);
    this._escapedBlockEndPattern = constructMarkerRegex(this.escapedBlockEndMarkers);

    this.complexTagStartMarkers = AstBuilder.selectSuitableMarkers(this.complexTagStartMarkers);
    this._complexStartTagPattern = constructMarkerRegex(this.complexTagStartMarkers);

    this.complexTagEndMarkers = AstBuilder.selectSuitableMarkers(this.complexTagEndMarkers);
    this._complexEndTagPattern = constructMarkerRegex(this.complexTagEndMarkers);
}

AstBuilder.prototype.parse = function(source, srcPath) {
    // reset.
    this._srcPath = srcPath;
    this._nodes = [];
    this._peekIdx = 0;

    const splitSource = utils.splitIntoLines(source, true);
    for (let i = 0; i < splitSource.length; i+=2) {
        const line = splitSource.get[i];
        const terminator = splitSource[i + 1];
        
        const n = {
            simpleContent: line,
            attrs: {
                lineSep: terminator
            }
        };
        n.attrs.type = AstBuilder.TYPE_TEXT_NODE;
        n.attrs.lineNum = Math.floor(i/2) + 1;
        n.attrs.indent = utils.determineIndent(line);
        this._nodes.push(n);
    }
    const rootContent = [];
    let child;
    while (child = this._matchAny()) {
        rootContent.push(child);
    }
    return rootContent;
};

AstBuilder.prototype._peek = function() {
    if (this._peekIdx < this._nodes.length) {
        return this._nodes[this._peekIdx];
    }
    return null;
}

AstBuilder.prototype._consume = function() {
    return this._nodes[this._peekIdx++];
}

AstBuilder.prototype._abort = function(n, msg) {
    let srcPathDesc = "";
    if (this._srcPath) {
        srcPathDesc = "in " + this._srcPath + " ";
    }
    let lineNumDesc = "";
    if (n) {
        lineNumDesc = "at llne " + n.attrs.lineNum + " ";
    }
    throw new Error(srcPathDesc + lineNumDesc + msg);
}

AstBuilder.prototype._matchAny = function() {
    let n = this._peek();
    if (!n) {
        return null;
    }
    if (findRegexMatch(this._complexEndTagPattern, n.simpleContent.substring(
            n.attrs.indent.length))) {
        throw this._abort(n, "encountered complex end tag without " +
            "matching start tag");
    }
    n = this._matchComplexTag();
    if (!n) {
        n = this._matchEscapedBlock();
    }
    if (!n ) {
        n = this._matchSimpleTag();
    }
    if (!n) {
        n = this._consume();
    }
    return n;
}

AstBuilder.prototype._matchComplexTag = function() {
    let n = this._peek();
    let valueMinusIndent = n.simpleContent.substring(n.attrs.indent.length);
    let m = findRegexMatch(this._complexStartTagPattern, valueMinusIndent);
    if (!m) {
        return null;
    }
    this._consume();
    n.simpleContent = null;
    n.attrs.type = AstBuilder.TYPE_COMPLEX_TAG;
    n.attrs.marker = m[1];
    n.attrs.markerAftermath = valueMinusIndent.substring(m[0].length);
    n.complexContent = [];
    const parent = n;
    while (n = peek()) {
        valueMinusIndent = n.simpleContent.substring(n.attrs.indent.length);
        m = findRegexMatch(this._complexEndTagPattern, valueMinusIndent);
        if (m) {
            break;
        }
        parent.complexContent.push(this._matchAny());
    }
    if (!n) {
        throw this._abort(parent, "matching complex end tag not found");
    }
    this._consume();
    parent.attrs.endIndent = n.attrs.indent;
    parent.attrs.endLineSep = n.attrs.lineSep;
    parent.attrs.endMarker = m[1];
    parent.attrs.endMarkerAftermath = valueMinusIndent.substring(m[0].length);
    return parent;
}

AstBuilder.prototype._matchEscapedBlock = function() {
    let n = this._peek();
    let valueMinusIndent = n.simpleContent.substring(n.attrs.indent.length);
    let m = findRegexMatch(this._escapedBlockStartPattern, valueMinusIndent);
    if (!m) {
        return null;
    }
    this._consume();
    n.simpleContent = null;
    n.attrs.type = AstBuilder.TYPE_ESCAPED_BLOCK;
    n.attrs.marker = m[1];
    n.attrs.markerAftermath = valueMinusIndent.substring(m[0].length);
    n.complexContent = [];
    const parent = n;
    const targetMarkerAftermath = parent.attrs.markerAftermath;
    while (n = this._peek()) {
        valueMinusIndent = n.simpleContent.substring(n.attrs.indent.length);
        m = findRegexMatch(this._escapedBlockEndPattern, valueMinusIndent);
        if (m) {
            markerAftermath = valueMinusIndent.substring(m[0].length);
            if (markerAftermath == targetMarkerAftermath) {
                break;
            }
        }
        parent.complexContent.push(n);
    }
    if (!n) {
        throw this._abort(parent, "matching escaped block end tag not found");
    }
    this._consume();
    parent.attrs.endIndent = n.attrs.indent;
    parent.attrs.endLineSep = n.attrs.lineSep;
    parent.attrs.endMarker = m[1];
    return parent;
}

AstBuilder.prototype._matchSimpleTag = function() {
    const n = this._peek();
    const valueMinusIndent = n.simpleContent.substring(
        n.attrs.indent.length);
    const m = findRegexMatch(this._simpleTagPattern, valueMinusIndent);
    if (!m) {
        return null;
    }
    this._consume();
    n.simpleContent = null;
    n.attrs.type = AstBuilder.TYPE_SIMPLE_TAG;
    n.attrs.marker = m[1];
    n.attrs.markerAftermath = valueMinusIndent.substring(m[0].length);
    return n;
}

module.exports = AstBuilder;