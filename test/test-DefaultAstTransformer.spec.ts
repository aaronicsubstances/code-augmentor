import { assert } from "chai";
import AstBuilder from "../src/AstBuilder";

import DefaultAstTransformer from "../src/DefaultAstTransformer";
import {
    AugmentingCodeDescriptor,
    DefaultAstTransformSpec,
    GeneratedCodeDescriptor,
    GeneratedCodePart,
    SourceCodeAst
} from "../src/types";

function determineAstNodePath(root: { children: any[] }, n: any) {
    if (root === n) {
        return [];
    }
    const p = new Array<number>();
    determineAstNodeSubpath(root, n, p);
    if (p.length > 0) {
        return p;
    }
    return null;
}

// precondition: root is different from n.
function determineAstNodeSubpath(parent: { children: any[] }, n: any,
        dest: Array<number>) {
    if (!parent || !parent.children) {
        return;
    }
    for (let i = 0; i < parent.children.length; i++) {
        const candidate = parent.children[i];
        if (candidate === n) {
            dest.push(i);
            break;
        }
        const currLen = dest.length;
        determineAstNodeSubpath(candidate, n, dest);
        if (dest.length > currLen) {
            break;
        }
    }
}

interface AugmentingCodeDescriptorClone {
    nodePath: number[] | null;
    nestedBlockUsed: boolean;
    lineNumber: number;
    markerAftermath: string;
    args: any[];
    argsExclEndIdxInParentNode: number;
    endMarkerAftermath: string | null;
    endArgs: any[] | null;
    endArgsExclEndIdxInParentNode: number;
    parentIndex: number | null;
    childIndices: number[] | null;
}

function flattenAugCodeTrees(rootNode: SourceCodeAst,
        augCodes: AugmentingCodeDescriptor[]) {
    const flattened = new Array<AugmentingCodeDescriptor>();
    for (const augCode of augCodes) {
        flattenAugCodeTree(augCode, flattened);
    }
    // now clone each entry, converting parentNode/indexInParentNode into
    // node paths, converting parent and children into indices in
    // flattened array.
    const clones = new Array<AugmentingCodeDescriptorClone>();
    for (const augCode of flattened) {
        const c: AugmentingCodeDescriptorClone = {
            nodePath: null,
            nestedBlockUsed: augCode.nestedBlockUsed,
            lineNumber: augCode.lineNumber,
            markerAftermath: augCode.markerAftermath,
            args: augCode.args,
            argsExclEndIdxInParentNode: augCode.argsExclEndIdxInParentNode,
            endMarkerAftermath: augCode.endMarkerAftermath,
            endArgs: augCode.endArgs,
            endArgsExclEndIdxInParentNode: augCode.endArgsExclEndIdxInParentNode,
            parentIndex: null,
            childIndices: null
        };
        if (augCode.parentNode) {
            const p = determineAstNodePath(rootNode, augCode.parentNode);
            if (p) {
                p.push(augCode.idxInParentNode);
                c.nodePath = p;
            }
        }
        if (augCode.parent) {
            c.parentIndex = flattened.indexOf(augCode.parent);
        }
        if (augCode.children) {
            c.childIndices = augCode.children.map(x => flattened.indexOf(x));
        }
        clones.push(c);
    }
    return clones;
}

function flattenAugCodeTree(augCode: AugmentingCodeDescriptor,
        dest: AugmentingCodeDescriptor[]) {
    dest.push(augCode);
    if (augCode.children) {
        augCode.children.forEach(a => flattenAugCodeTree(a, dest));
    }
}

describe("DefaultAstTransformer", function() {

    describe("#extractAugCodes", function() {
        it("should pass with input 0", function() {
            const instance = new DefaultAstTransformer();
            instance.augCodeArgMarkers = ["D"];
            instance.augCodeJsonArgMarkers = ["J"];
            instance.augCodeArgSepMarkers = [",", ";"];
            const parentNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: []
            };
            const expected = [];
            const actual = instance.extractAugCodes(parentNode);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 1", function() {
            const instance = new DefaultAstTransformer();
            instance.augCodeMarkers = ["A:", "A*"];
            instance.augCodeArgMarkers = ["D-"];
            instance.augCodeJsonArgMarkers = ["J>"];
            instance.augCodeArgSepMarkers = [",", ";"];
            const parentNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "could",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "we",
                        markerAftermath: " did",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "A:",
                        markerAftermath: " did",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "D-",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "\t",
                        marker: "J>",
                        markerAftermath: "12",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "goods",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "A:",
                        markerAftermath: "do",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        indent: "",
                        marker: "A*",
                        markerAftermath: "",
                        lineSep: "\n",
                        endIndent: " ",
                        endMarker: "A+",
                        endMarkerAftermath: "done",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: ";",
                                markerAftermath: "",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "no",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "A:",
                                markerAftermath: "tan",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "G:",
                                markerAftermath: " did",
                                lineSep: "\n",
                            }
                        ]
                    },
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "sure",
                        indent: "   ",
                        marker: "G*",
                        lineSep: "\r\n",
                        endIndent: " ",
                        endMarker: "G+",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "y",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "e",
                                lineSep: "\n",
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "s",
                                lineSep: "\n",
                            }
                        ]
                    },
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        indent: "",
                        marker: "shooter",
                        markerAftermath: " do",
                        lineSep: "\n",
                        endMarker: "shooter+",
                        endMarkerAftermath: " done",
                        endLineSep: "\n",
                        children: []
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "A:",
                        markerAftermath: "cos",
                        lineSep: "\n"
                    }
                ]
            };
            const expected: AugmentingCodeDescriptorClone[] = [
                {
                    nodePath: [2],
                    nestedBlockUsed: false,
                    lineNumber: 3,
                    markerAftermath: " did",
                    args: ["", 12],
                    argsExclEndIdxInParentNode: 5,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: null,
                    childIndices: []
                },
                {
                    nodePath: [6],
                    nestedBlockUsed: false,
                    lineNumber: 7,
                    markerAftermath: "do",
                    args: [],
                    argsExclEndIdxInParentNode: 7,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: null,
                    childIndices: []
                },
                {
                    nodePath: [7],
                    nestedBlockUsed: true,
                    lineNumber: 8,
                    markerAftermath: "",
                    args: [],
                    argsExclEndIdxInParentNode: 1,
                    endMarkerAftermath: "done",
                    endArgs: [],
                    endArgsExclEndIdxInParentNode: 8,
                    parentIndex: null,
                    childIndices: [3]
                },
                {
                    nodePath: [7, 2],
                    nestedBlockUsed: false,
                    lineNumber: 11,
                    markerAftermath: "tan",
                    args: [],
                    argsExclEndIdxInParentNode: 3,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: 2,
                    childIndices: []
                },
                {
                    nodePath: [10],
                    nestedBlockUsed: false,
                    lineNumber: 21,
                    markerAftermath: "cos",
                    args: [],
                    argsExclEndIdxInParentNode: 11,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: null,
                    childIndices: []
                }
            ];
            const actual = instance.extractAugCodes(parentNode);
            const actualFlattened = flattenAugCodeTrees(parentNode, actual);
            assert.deepEqual(actualFlattened, expected);
        });
    });

    describe("#_extractAugCodeArgs", function() {
        it("should pass with input 0", function() {
            const instance = new DefaultAstTransformer();
            const parentNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: []
            };
            const startIndex = 0;
            const expected = {
                args: [],
                exclEndIdx: 0
            };
            const actual = instance._extractAugCodeArgs(parentNode, startIndex);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 1", function() {
            const instance = new DefaultAstTransformer();
            const parentNode: any = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: null
            };
            const startIndex = 10;
            const expected = {
                args: [],
                exclEndIdx: 0
            };
            const actual = instance._extractAugCodeArgs(parentNode, startIndex);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 2", function() {
            const instance = new DefaultAstTransformer();
            instance.augCodeArgMarkers = ["D"];
            instance.augCodeJsonArgMarkers = ["J"];
            instance.augCodeArgSepMarkers = [",", ";"];
            const parentNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "D",
                        markerAftermath: "i",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "D",
                        markerAftermath: "i",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "D",
                        lineSep: "\n"
                    }
                ]
            };
            const startIndex = 1;
            const expected = {
                args: ["i"],
                exclEndIdx: 2
            };
            const actual = instance._extractAugCodeArgs(parentNode, startIndex);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 3", function() {
            const instance = new DefaultAstTransformer();
            instance.augCodeArgMarkers = ["J", ",", "D", ";"];
            instance.augCodeJsonArgMarkers = ["J"];
            instance.augCodeArgSepMarkers = [",", ";"];
            const parentNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "D",
                        markerAftermath: "i",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "D",
                        markerAftermath: "i",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: ",",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "J",
                        markerAftermath: "true",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "D",
                        lineSep: "\n"
                    }
                ]
            };
            const startIndex = 1;
            const expected = {
                args: ["i", true],
                exclEndIdx: 4
            };
            const actual = instance._extractAugCodeArgs(parentNode, startIndex);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 4", function() {
            const instance = new DefaultAstTransformer();
            instance.augCodeArgMarkers = ["D", "J"];
            instance.augCodeArgSepMarkers = [",", ";"];
            const parentNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "D",
                        markerAftermath: "i",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "D",
                        markerAftermath: "i",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: ",",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "J",
                        markerAftermath: "true",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "D",
                        lineSep: "\n"
                    }
                ]
            };
            const startIndex = 1;
            const expected = {
                args: ["i", "true"],
                exclEndIdx: 4
            };
            const actual = instance._extractAugCodeArgs(parentNode, startIndex);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#_repairSplitCrLfs", function() {
        it(`it should pass successfully with input 0`, function() {
            const contentParts = new Array<GeneratedCodePart>();
            const expected = new Array<GeneratedCodePart>();
            DefaultAstTransformer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`it should pass successfully with input 1`, function() {
            const contentParts = [{
                content: "",
                exempt: false
            }];
            const expected = [{
                content: "",
                exempt: false
            }];
            DefaultAstTransformer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`it should pass successfully with input 2`, function() {
            const contentParts = [{
                content: "aab\r",
                exempt: true
            }];
            const expected = [{
                content: "aab\r",
                exempt: true
            }];
            DefaultAstTransformer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`it should pass successfully with input 3`, function() {
            const contentParts = [{
                content: "aab\r",
                exempt: true
            }, {
                content: "\ncd",
                exempt: false
            }];
            const expected = [{
                content: "aab\r\n",
                exempt: true
            }, {
                content: "cd",
                exempt: false
            }];
            DefaultAstTransformer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`it should pass successfully with input 4`, function() {
            const contentParts = [{
                content: "aab\r",
                exempt: true
            }, {
                content: "\ncd\r",
                exempt: false
            }, {
                content: "\nefgh",
                exempt: true
            }, {
                content: "end",
                exempt: false
            }];
            const expected = [{
                content: "aab\r\n",
                exempt: true
            }, {
                content: "cd\r\n",
                exempt: false
            }, {
                content: "efgh",
                exempt: true
            }, {
                content: "end",
                exempt: false
            }];
            DefaultAstTransformer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`it should pass successfully with input 5`, function() {
            const contentParts = [{
                content: "aab\r",
                exempt: true
            }, {
                content: "cd\n",
                exempt: false
            }, {
                content: "\nefgh\r\n",
                exempt: true
            }, {
                content: "\rend",
                exempt: false
            }];
            const expected = [{
                content: "aab\r",
                exempt: true
            }, {
                content: "cd\n",
                exempt: false
            }, {
                content: "\nefgh\r\n",
                exempt: true
            }, {
                content: "\rend",
                exempt: false
            }];
            DefaultAstTransformer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`it should pass successfully with input 6`, function() {
            const contentParts: any = [{
                content: null
            }, null, {
                content: "\ncd",
                exempt: false
            }];
            const expected: any = [{
                content: null
            }, null, {
                content: "\ncd",
                exempt: false
            }];
            DefaultAstTransformer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
    });

    describe("#extractLinesAndTerminators", function() {
        it(`should pass correctly for input 0`, function() {
            const indent = '';
            const lineSeparator = '';
            const contentParts = [{
                content: "",
                exempt: true
            }, {
                content: "",
                exempt: false
            }, {
                content: "abc",
                exempt: false
            }, {
                content: "de\nf",
                exempt: true
            }, {
                content: "\r\n\n gh\ni",
                exempt: false
            }, {
                content: "",
                exempt: false
            }];
            const expected = ["abcde", "\n",
                "f", "\r\n",
                "", "\n",
                " gh", "\n",
                "i", ""];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 1', function() {
            const indent = ' ';
            const lineSeparator = '\r\n';
            const contentParts = [{
                content: "\nabcd\nefgh\n",
                exempt: false
            }];
            const expected = [
                "", "\r\n",
                " abcd", "\r\n",
                " efgh", "\r\n"];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 2', function() {
            const indent = '  ';
            const lineSeparator = '\n';
            const contentParts = [{
                content: "abcd",
                exempt: true
            }, {
                content: "e\r\nf",
                exempt: true
            }, {
                content: "g\rh",
                exempt: true
            }];
            const expected = [
                "abcde", "\r\n",
                "fg", "\r",
                "h", ""];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 3', function() {
            const indent = '  ';
            const lineSeparator = '\n';
            const contentParts = [{
                content: "abcd",
                exempt: false
            }, {
                content: "e\r\nf",
                exempt: true
            }, {
                content: "g\rh",
                exempt: true
            }];
            const expected = [
                "  abcde", "\r\n",
                "fg", "\r",
                "h", ""];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 4', function() {
            const indent = '  ';
            const lineSeparator = null;
            const contentParts = [{
                content: "",
                exempt: false
            }, {
                content: "",
                exempt: false
            }, {
                content: "abcd",
                exempt: false
            }, {
                content: "e\r\nf",
                exempt: true
            }, {
                content: "g\rh",
                exempt: true
            }];
            const expected = [
                "  abcde", "\r\n",
                "fg", "\r",
                "h", ""];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 5', function() {
            const indent = ' ';
            const lineSeparator = null;
            const contentParts = [{
                content: "\n",
                exempt: false
            }, {
                content: "\n\n",
                exempt: false
            }, {
                content: "\r\n\r\n",
                exempt: false
            }];
            const expected = [
                "", "\n",
                "", "\n",
                "", "\n",
                "", "\r\n",
                "", "\r\n"];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 6', function() {
            const indent = '\t';
            const lineSeparator = "";
            const contentParts = [{
                content: "",
                exempt: true
            }, {
                content: "",
                exempt: false
            }, {
                content: "abc",
                exempt: false
            }, {
                content: "de\nf",
                exempt: true
            }, {
                content: "\r\n\n gh\ni",
                exempt: false
            }, {
                content: "",
                exempt: false
            }];
            const expected = [
                "abcde", "\n",
                "f", "\r\n",
                "", "\n",
                "\t gh", "\n",
                "\ti", ""];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 7', function() {
            const indent = null;
            const lineSeparator = "";
            const contentParts = [{
                content: "",
                exempt: true
            }, {
                content: "",
                exempt: false
            }, {
                content: "abc",
                exempt: false
            }, {
                content: "de\nf",
                exempt: true
            }, {
                content: "\r\n\n gh\ni",
                exempt: false
            }, {
                content: "",
                exempt: false
            }];
            const expected = [
                "abcde", "\n",
                "f", "\r\n",
                "", "\n",
                " gh", "\n",
                "i", ""];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 8', function() {
            const indent = null;
            const lineSeparator = "";
            const contentParts = [];
            const expected = [];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 9', function() {
            const indent = null;
            const lineSeparator = "\n";
            const contentParts = [{
                content: "",
                exempt: true
            }];
            const expected = ["", ""];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 10', function() {
            const indent = null;
            const lineSeparator = "\n";
            const contentParts = null;
            const expected = [];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 11', function() {
            const indent = null;
            const lineSeparator = "";
            const contentParts: any = [{
                content: "",
                exempt: true
            }, null];
            const expected = ["", ''];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 12', function() {
            const indent = '  ';
            const lineSeparator = '\n';
            const contentParts = [{
                content: "abcd",
                exempt: false
            }, {
                content: "e\r\nf",
                exempt: false
            }, {
                content: "g\rh",
                exempt: false
            }];
            const expected = [
                "  abcde", "\n",
                "  fg", "\n",
                "  h", "\n"];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 13', function() {
            const indent = '';
            const lineSeparator = '';
            const contentParts = [{
                content: "abcd",
                exempt: false
            }, {
                content: "e\r\nf",
                exempt: false
            }, {
                content: "g\rh\n",
                exempt: false
            }];
            const expected = [
                "abcde", "\r\n",
                "fg", "\r",
                "h", "\n"];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 14', function() {
            const indent = '';
            const lineSeparator = '\n';
            const contentParts = [{
                content: "abcd",
                exempt: false
            }, {
                content: "e\r\nf",
                exempt: false
            }, {
                content: "g\rh",
                exempt: false
            }];
            const expected = [
                "abcde", "\n",
                "fg", "\n",
                "h", "\n"];
            const actual = DefaultAstTransformer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#_consolidateAugCodeArgs", function() {
        it("should pass with input 0", function() {
            const args = [];
            const expected = [];
            const actual = DefaultAstTransformer._consolidateAugCodeArgs(args);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 1", function() {
            const args = ["", "\n", false,
                "56", "\r\n", false];
            const expected = ["\n56"];
            const actual = DefaultAstTransformer._consolidateAugCodeArgs(args);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 2", function() {
            const args = [
                null,
                "null", "\n", true,
                null,
                " c ", "\r\n", false];
            const expected = [null, " c "];
            const actual = DefaultAstTransformer._consolidateAugCodeArgs(args);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 3", function() {
            const args = [
                "the", "\n", false,
                null,
                " gist", "\r\n", false,
                ".", "\r\n", false,
                "[4", "\n", true,
                ",{\"k\":\"see\"}", "\n", true,
                ",true]", "", true,
                null,
                "{\"data\":null}", "", true];
            const expected = [
                "the",
                " gist\r\n.",
                [ 4.0, { k: "see" }, true ],
                { data: null }
            ];
            const actual = DefaultAstTransformer._consolidateAugCodeArgs(args);
            assert.deepEqual(actual, expected);
        });
        it("should fail", function() {
            const args = ["", "\n", true,
                "  ", "\r\n", true];
            assert.throws(function() {
                DefaultAstTransformer._consolidateAugCodeArgs(args);
            });
        });
    });

    describe("#_createGenCodeNode", function() {
        it("should pass with input 0", function() {
            const instance = new DefaultAstTransformer();
            instance.defaultGenCodeInlineMarker = "g_inline";
            instance.defaultGenCodeStartMarker = "g_start:";
            instance.defaultGenCodeEndMarker = "g_end:";
            const genCodeLines = [
                "in", "\n",
                " the ", "\r\n",
                "beginning", "\r\n"
            ];
            const useInlineMarker = false;
            const genCodeSection: GeneratedCodeDescriptor | null = null;
            const defaultIndent = "\t";
            const defaultLineSep = "\r";
            const expected = {
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: "",
                indent: "\t",
                endIndent: "\t",
                marker: "g_start:",
                endMarker: "g_end:",
                lineSep: "\r",
                endLineSep: "\r",
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "in",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: " the ",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "beginning",
                        lineSep: "\r\n"
                    }
                ]
            };
            const actual = instance._createGenCodeNode(genCodeLines, useInlineMarker,
                genCodeSection, defaultIndent, defaultLineSep);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 1", function() {
            const instance = new DefaultAstTransformer();
            instance.defaultGenCodeStartMarker = "_start:";
            instance.defaultGenCodeEndMarker = "_end:";
            const genCodeLines: string[] = [
                "in", "\n",
                " the ", "\r\n",
                "beginning", "\r\n"
            ];
            const useInlineMarker = false;
            const genCodeSection: any = {
                nestedBlockUsed: false
            };
            const defaultIndent = "";
            const defaultLineSep = "\r\n";
            const expected = {
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: "",
                indent: "",
                endIndent: "",
                marker: "_start:",
                endMarker: "_end:",
                lineSep: "\r\n",
                endLineSep: "\r\n",
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "in",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: " the ",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "beginning",
                        lineSep: "\r\n"
                    }
                ]
            };
            const actual = instance._createGenCodeNode(genCodeLines, useInlineMarker,
                genCodeSection, defaultIndent, defaultLineSep);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 2", function() {
            const instance = new DefaultAstTransformer();
            instance.defaultGenCodeInlineMarker = "g_inline";
            const genCodeLines = ["in", "\n"];
            const useInlineMarker = true;
            const genCodeSection: GeneratedCodeDescriptor | null = null;
            const defaultIndent = "";
            const defaultLineSep = "\r\n";
            const expected = {
                type: AstBuilder.TYPE_DECORATED_LINE,
                indent: "",
                marker: "g_inline",
                markerAftermath: "in",
                lineSep: "\r\n",
            };
            const actual = instance._createGenCodeNode(genCodeLines, useInlineMarker,
                genCodeSection, defaultIndent, defaultLineSep);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 3", function() {
            const instance = new DefaultAstTransformer();
            instance.defaultGenCodeInlineMarker = "_inline";
            instance.defaultGenCodeStartMarker = "_start:";
            instance.defaultGenCodeEndMarker = "_end:";
            const genCodeLines = [
                "in", "\n",
                " the ", "\r\n",
                "beginning", "\r\n"
            ];
            const useInlineMarker = true;
            const genCodeSection: any = {
                nestedBlockUsed: true
            };
            const defaultIndent = "\t\t";
            const defaultLineSep = "\r";
            const expected = {
                type: AstBuilder.TYPE_DECORATED_LINE,
                indent: "\t\t",
                marker: "_inline",
                markerAftermath: "in",
                lineSep: "\r",
            };
            const actual = instance._createGenCodeNode(genCodeLines, useInlineMarker,
                genCodeSection, defaultIndent, defaultLineSep);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 4", function() {
            const instance = new DefaultAstTransformer();
            const genCodeLines= new Array<string>();
            const useInlineMarker = true;
            const genCodeSection: GeneratedCodeDescriptor = {
                parentNode: {
                    type: AstBuilder.TYPE_SOURCE_CODE,
                    children: [
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            indent: " ",
                            marker: "jui",
                            markerAftermath: "ce",
                            lineSep: "\n"
                        }
                    ]
                },
                idxInParentNode: 0,
                nestedBlockUsed: false
            };
            const defaultIndent = null;
            const defaultLineSep = null;
            const expected = {
                type: AstBuilder.TYPE_DECORATED_LINE,
                indent: " ",
                marker: "jui",
                markerAftermath: "",
                lineSep: "\n",
            };
            const actual = instance._createGenCodeNode(genCodeLines, useInlineMarker,
                genCodeSection, defaultIndent, defaultLineSep);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 5", function() {
            const instance = new DefaultAstTransformer();
            const genCodeLines = [
                "in", "\n",
                " the ", "\r\n",
                "beginning", ""
            ];
            const useInlineMarker = false;
            const genCodeSection: GeneratedCodeDescriptor = {
                parentNode: {
                    type: AstBuilder.TYPE_NESTED_BLOCK,
                    children: [
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            indent: "",
                            marker: "i",
                            markerAftermath: "eb",
                            lineSep: "\r"
                        },
                        {
                            type: AstBuilder.TYPE_ESCAPED_BLOCK,
                            markerAftermath: "ce",
                            indent: " ",
                            marker: "jui",
                            lineSep: "\n",
                            endIndent: "\t",
                            endMarker: "dri",
                            endLineSep: "\r\n"
                        }
                    ]
                },
                idxInParentNode: 1,
                nestedBlockUsed: true
            };
            const defaultIndent = null;
            const defaultLineSep = null;
            const expected = {
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: "ce",
                indent: " ",
                marker: "jui",
                lineSep: "\n",
                endIndent: "\t",
                endMarker: "dri",
                endLineSep: "\r\n",
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "in",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: " the ",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "beginning",
                        lineSep: "\n"
                    }
                ]
            };
            const actual = instance._createGenCodeNode(genCodeLines, useInlineMarker,
                genCodeSection, defaultIndent, defaultLineSep);
            assert.deepEqual(actual, expected);
        });

        it("should fail due to missing default inline marker", function() {
            const instance = new DefaultAstTransformer();
            instance.defaultGenCodeStartMarker = "8";
            instance.defaultGenCodeEndMarker = "d";
            const genCodeLines = [];
            const useInlineMarker = true;
            const genCodeSection = null;
            const defaultIndent = null;
            const defaultLineSep = null;
            assert.throws(function() {
                instance._createGenCodeNode(genCodeLines, useInlineMarker,
                    genCodeSection, defaultIndent, defaultLineSep);
            }, "inline marker");
        });

        it("should fail due to missing default start marker", function() {
            const instance = new DefaultAstTransformer();
            instance.defaultGenCodeInlineMarker = "d";
            instance.defaultGenCodeEndMarker = "8";
            const genCodeLines = [
                "in", "\n",
                " the ", "\r\n",
                "beginning", ""
            ];
            const useInlineMarker = false;
            const genCodeSection = null;
            const defaultIndent = null;
            const defaultLineSep = null;
            assert.throws(function() {
                instance._createGenCodeNode(genCodeLines, useInlineMarker,
                    genCodeSection, defaultIndent, defaultLineSep);
            }, "start marker");
        });

        it("should fail due to missing default end marker", function() {
            const instance = new DefaultAstTransformer();
            instance.defaultGenCodeInlineMarker = "d";
            instance.defaultGenCodeStartMarker = "8";
            const genCodeLines = [
                "in", "\n",
                " the ", "\r\n",
                "beginning", ""
            ];
            const useInlineMarker = false;
            const genCodeSection = null;
            const defaultIndent = null;
            const defaultLineSep = null;
            assert.throws(function() {
                instance._createGenCodeNode(genCodeLines, useInlineMarker,
                    genCodeSection, defaultIndent, defaultLineSep);
            }, "end marker");
        });
    });

    describe("#performTransformations", function() {
        it("it should pass with empty input", function() {
            DefaultAstTransformer.performTransformations([]);
        });
        it("it should pass with no additions", function() {
            const node1 = {
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "0",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "1",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "2",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "3",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "4",
                        markerAftermath: "d",
                        lineSep: "\n",
                    }
                ]
            };
            const transformSpecs: DefaultAstTransformSpec[] = [
                {
                    node: node1,
                    childIndex: 0,
                    childToInsert: null,
                    replacementChild: null,
                    performDeletion: true
                },
                {
                    node: node1,
                    childIndex: 2,
                    childToInsert: null,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "a",
                        lineSep: "\r\n"
                    },
                    performDeletion: false
                },
                {
                    node: node1,
                    childIndex: 3,
                    childToInsert: null,
                    replacementChild: null,
                    performDeletion: true
                },
                {
                    node: node1,
                    childIndex: 4,
                    childToInsert: null,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "b",
                        lineSep: "\r\n"
                    },
                    performDeletion: false
                },
                {
                    node: node1,
                    childIndex: 4,
                    childToInsert: null,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "c",
                        lineSep: "\n"
                    },
                    performDeletion: false
                }
            ];
            const expected = {
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "1",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "a",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "c",
                        lineSep: "\n"
                    }
                ]
            };
            DefaultAstTransformer.performTransformations(transformSpecs);
            assert.deepEqual(node1, expected);
        });
        it("it should pass with some additions", function() {
            const node1 = {
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "0",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "1",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "2",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "3",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "4",
                        markerAftermath: "d",
                        lineSep: "\n",
                    }
                ]
            };
            const node2: any = {
                type: AstBuilder.TYPE_NESTED_BLOCK
            };
            const transformSpecs: DefaultAstTransformSpec[] = [
                {
                    node: node1,
                    childIndex: 0,
                    childToInsert: null,
                    replacementChild: null,
                    performDeletion: true
                },
                {
                    node: node1,
                    childIndex: 2,
                    childToInsert: null,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "a",
                        lineSep: "\r\n"
                    },
                    performDeletion: false
                },
                {
                    node: node1,
                    childIndex: 3,
                    childToInsert: null,
                    replacementChild: null,
                    performDeletion: true
                },
                {
                    node: node1,
                    childIndex: 4,
                    childToInsert: null,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "b",
                        lineSep: "\r\n"
                    },
                    performDeletion: false
                },
                {
                    node: node1,
                    childIndex: 4,
                    childToInsert: null,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "c",
                        lineSep: "\n"
                    },
                    performDeletion: false
                },
                {
                    node: node1,
                    childIndex: 5,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "j",
                        lineSep: "\n"
                    },
                    replacementChild: null,
                    performDeletion: false
                },
                {
                    node: node1,
                    childIndex: 5,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "m",
                        lineSep: "\n"
                    },
                    replacementChild: null,
                    performDeletion: false
                },
                {
                    node: node2,
                    childIndex: 0,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "n",
                        lineSep: "\r\n"
                    },
                    replacementChild: null,
                    performDeletion: false
                }
            ];
            const expected1 = {
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "1",
                        markerAftermath: "",
                        lineSep: "\n",
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "a",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "c",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "j",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "m",
                        lineSep: "\n"
                    },
                ]
            };
            const expected2 = {
                type: AstBuilder.TYPE_NESTED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "n",
                        lineSep: "\r\n"
                    },
                ]
            };
            DefaultAstTransformer.performTransformations(transformSpecs);
            assert.deepEqual(node1, expected1);
            assert.deepEqual(node2, expected2);
        });
    });
});
