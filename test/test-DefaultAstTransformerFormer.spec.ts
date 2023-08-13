import { assert } from "chai";
import { AstBuilder } from "../src/AstBuilder";

import { DefaultAstTransformerFormer } from "./DefaultAstTransformerFormer";
import {
    AugmentingCodeDescriptor2,
    DefaultAstTransformSpec,
    GeneratedCode,
    GeneratedCodeDescriptor,
    GeneratedCodePart,
    GeneratedCodeSectionTransform,
    NestedBlockAstNode,
    SourceCodeAst
} from "./types";

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
            dest.splice(currLen, 0, i);
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
        augCodes: AugmentingCodeDescriptor2[]) {
    const flattened = new Array<AugmentingCodeDescriptor2>();
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

function flattenAugCodeTree(augCode: AugmentingCodeDescriptor2,
        dest: AugmentingCodeDescriptor2[]) {
    dest.push(augCode);
    if (augCode.children) {
        augCode.children.forEach(a => flattenAugCodeTree(a, dest));
    }
}

describe("DefaultAstTransformerFormer", function() {

    describe("#extractAugCodes", function() {
        it("should pass with input 0", function() {
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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

        it("should pass with input 2", function() {
            const instance = new DefaultAstTransformerFormer();
            instance.augCodeMarkers = ["indu", "beig"];
            instance.augCodeJsonArgMarkers = ["notation"];
            instance.augCodeArgSepMarkers = ["sep,,"];
            instance.augCodeArgMarkers = ["input_arg"];
            instance.genCodeMarkers = ["[[!", "{{"];
            const parentNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        indent: "  ",
                        marker: "beig",
                        markerAftermath: "",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "es",
                        endMarkerAftermath: "kop",
                        endLineSep: "\r\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "beig",
                                markerAftermath: "0",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "c::",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "\t",
                                marker: "notation",
                                markerAftermath: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "ad",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: " ",
                                marker: "[[!",
                                markerAftermath: "4",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: " ",
                                marker: "indu",
                                markerAftermath: "5",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "o",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "input_arg",
                                markerAftermath: "7",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                                markerAftermath: "8",
                                indent: "",
                                marker: "{{",
                                lineSep: "\n",
                                endIndent: "\t",
                                endMarker: "p",
                                endLineSep: "\r\n",
                                children: []
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "beig",
                                markerAftermath: "9",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "larop",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "\t\t",
                                marker: "sep,,",
                                markerAftermath: "11",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                                markerAftermath: "12",
                                indent: "",
                                marker: "[[!",
                                lineSep: "\n",
                                endIndent: "",
                                endMarker: "gof",
                                endLineSep: "\n",
                                children: []
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "indu",
                                markerAftermath: "13",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "larop",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_NESTED_BLOCK,
                                markerAftermath: "15",
                                endMarkerAftermath: "v",
                                indent: "",
                                marker: "indu",
                                lineSep: "\n",
                                endIndent: "",
                                endMarker: "gof",
                                endLineSep: "\n",
                                children: [
                                    {
                                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                                        text: "o",
                                        lineSep: "\r\n"
                                    },
                                    {
                                        type: AstBuilder.TYPE_DECORATED_LINE,
                                        indent: " ",
                                        marker: "indu",
                                        markerAftermath: "1",
                                        lineSep: "\r\n"
                                    },
                                    {
                                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                                        text: "o",
                                        lineSep: "\r\n"
                                    },
                                    {
                                        type: AstBuilder.TYPE_DECORATED_LINE,
                                        indent: "",
                                        marker: "{{",
                                        markerAftermath: "3",
                                        lineSep: "\r\n"
                                    },
                                    {
                                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                                        text: "o",
                                        lineSep: "\r\n"
                                    }]
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "notation",
                                markerAftermath: "16",
                                lineSep: "\n"
                            },
                        ]
                    }
                ]
            };
            const expected: AugmentingCodeDescriptorClone[] = [
                {
                    nodePath: [0],
                    nestedBlockUsed: true,
                    lineNumber: 1,
                    markerAftermath: "",
                    args: [],
                    argsExclEndIdxInParentNode: 0,
                    endMarkerAftermath: "kop",
                    endArgs: [],
                    endArgsExclEndIdxInParentNode: 1,
                    parentIndex: null,
                    childIndices: [1, 2, 3, 4, 5]
                },
                {
                    nodePath: [0, 0],
                    nestedBlockUsed: false,
                    lineNumber: 2,
                    markerAftermath: "0",
                    args: [],
                    argsExclEndIdxInParentNode: 1,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: 0,
                    childIndices: []
                },
                {
                    nodePath: [0, 5],
                    nestedBlockUsed: false,
                    lineNumber: 7,
                    markerAftermath: "5",
                    args: [],
                    argsExclEndIdxInParentNode: 6,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: 0,
                    childIndices: []
                },
                {
                    nodePath: [0, 9],
                    nestedBlockUsed: false,
                    lineNumber: 12,
                    markerAftermath: "9",
                    args: [],
                    argsExclEndIdxInParentNode: 10,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: 0,
                    childIndices: []
                },
                {
                    nodePath: [0, 13],
                    nestedBlockUsed: false,
                    lineNumber: 17,
                    markerAftermath: "13",
                    args: [],
                    argsExclEndIdxInParentNode: 14,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: 0,
                    childIndices: []
                },
                {
                    nodePath: [0, 15],
                    nestedBlockUsed: true,
                    lineNumber: 19,
                    markerAftermath: "15",
                    args: [],
                    argsExclEndIdxInParentNode: 0,
                    endMarkerAftermath: "v",
                    endArgs: [16],
                    endArgsExclEndIdxInParentNode: 17,
                    parentIndex: 0,
                    childIndices: [6]
                },
                {
                    nodePath: [0, 15, 1],
                    nestedBlockUsed: false,
                    lineNumber: 21,
                    markerAftermath: "1",
                    args: [],
                    argsExclEndIdxInParentNode: 2,
                    endMarkerAftermath: null,
                    endArgs: null,
                    endArgsExclEndIdxInParentNode: -1,
                    parentIndex: 5,
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
        it(`should pass successfully with input 0`, function() {
            const contentParts = new Array<GeneratedCodePart>();
            const expected = new Array<GeneratedCodePart>();
            DefaultAstTransformerFormer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`should pass successfully with input 1`, function() {
            const contentParts = [{
                content: "",
                exempt: false
            }];
            const expected = [{
                content: "",
                exempt: false
            }];
            DefaultAstTransformerFormer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`should pass successfully with input 2`, function() {
            const contentParts = [{
                content: "aab\r",
                exempt: true
            }];
            const expected = [{
                content: "aab\r",
                exempt: true
            }];
            DefaultAstTransformerFormer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`should pass successfully with input 3`, function() {
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
            DefaultAstTransformerFormer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`should pass successfully with input 4`, function() {
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
            DefaultAstTransformerFormer._repairSplitCrLfs(contentParts);
            assert.deepEqual(contentParts, expected);
        });
        it(`should pass successfully with input 5`, function() {
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
            DefaultAstTransformerFormer._repairSplitCrLfs(contentParts);
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
            DefaultAstTransformerFormer._repairSplitCrLfs(contentParts);
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 8', function() {
            const indent = null;
            const lineSeparator = "";
            const contentParts = [];
            const expected = [];
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
        it('should pass correctly for input 10', function() {
            const indent = null;
            const lineSeparator = "\n";
            const contentParts = null;
            const expected = [];
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
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
            const actual = DefaultAstTransformerFormer.extractLinesAndTerminators(contentParts,
                indent, lineSeparator);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#_consolidateAugCodeArgs", function() {
        it("should pass with input 0", function() {
            const args = [];
            const expected = [];
            const actual = DefaultAstTransformerFormer._consolidateAugCodeArgs(args);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 1", function() {
            const args = ["", "\n", false,
                "56", "\r\n", false];
            const expected = ["\n56"];
            const actual = DefaultAstTransformerFormer._consolidateAugCodeArgs(args);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 2", function() {
            const args = [
                null,
                "null", "\n", true,
                null,
                " c ", "\r\n", false];
            const expected = [null, " c "];
            const actual = DefaultAstTransformerFormer._consolidateAugCodeArgs(args);
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
            const actual = DefaultAstTransformerFormer._consolidateAugCodeArgs(args);
            assert.deepEqual(actual, expected);
        });
        it("should fail", function() {
            const args = ["", "\n", true,
                "  ", "\r\n", true];
            assert.throws(function() {
                DefaultAstTransformerFormer._consolidateAugCodeArgs(args);
            });
        });
    });

    describe("#extractGenCodeSections", function() {
        it("should pass with input 0", function() {
            const parentNode: SourceCodeAst | NestedBlockAstNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [{
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "js",
                    markerAftermath: "",
                    lineSep: "\n"
                }]
            };
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 0,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 1,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = [];
            const instance = new DefaultAstTransformerFormer();
            const actual = instance.extractGenCodeSections(augCode);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 1", function() {
            const parentNode: SourceCodeAst | NestedBlockAstNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [{
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "js",
                    markerAftermath: "",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "g:",
                    markerAftermath: "",
                    lineSep: "\n"
                }]
            };
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 0,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 1,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = [{
                parentNode: parentNode,
                idxInParentNode: 1,
                nestedBlockUsed: false
            }];
            const instance = new DefaultAstTransformerFormer();
            instance.genCodeMarkers = ["g:"];
            const actual = instance.extractGenCodeSections(augCode);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 2", function() {
            const parentNode: SourceCodeAst | NestedBlockAstNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [{
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "js",
                    markerAftermath: "",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "g:",
                    markerAftermath: "",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "g8",
                    markerAftermath: "",
                    lineSep: "\n"
                }]
            };
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 0,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 1,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = [];
            const instance = new DefaultAstTransformerFormer();
            instance.augCodeMarkers = ["a8"];
            instance.augCodeJsonArgMarkers = ["j>"];
            instance.augCodeArgSepMarkers = ["g:"];
            instance.augCodeArgMarkers = ["a:"];
            instance.genCodeMarkers = ["g8", "g:"];
            const actual = instance.extractGenCodeSections(augCode);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 3", function() {
            const parentNode: SourceCodeAst | NestedBlockAstNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [{
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "js",
                    markerAftermath: "",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "i",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "",
                    indent: "",
                    marker: "g8",
                    lineSep: "\n",
                    endIndent: "",
                    endMarker: "p8",
                    endLineSep: "\n",
                    children: [
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "ef",
                            lineSep: "\n"
                        },
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "cd",
                            lineSep: "\n"
                        }
                    ]
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "ooods",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "g:",
                    markerAftermath: "",
                    lineSep: "\n"
                }]
            };
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 1,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 2,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = [{
                parentNode: parentNode,
                idxInParentNode: 3,
                nestedBlockUsed: true
            }];
            const instance = new DefaultAstTransformerFormer();
            instance.augCodeMarkers = ["js"];
            instance.augCodeJsonArgMarkers = ["j>"];
            instance.augCodeArgSepMarkers = ["g:"];
            instance.augCodeArgMarkers = ["a:"];
            instance.genCodeMarkers = ["g8", "g:"];
            const actual = instance.extractGenCodeSections(augCode);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 4", function() {
            const childNode1 = {
                type: AstBuilder.TYPE_NESTED_BLOCK,
                indent: "  ",
                marker: "js",
                markerAftermath: "",
                lineSep: "\n",
                endIndent: "",
                endMarker: "es",
                endMarkerAftermath: "kop",
                endLineSep: "\r\n",
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "js",
                        markerAftermath: ":dk",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "2",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "33",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "dkdl",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "a8",
                        markerAftermath: "p0",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "o",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "oods",
                        indent: "",
                        marker: "g8",
                        lineSep: "\n",
                        endIndent: "\t",
                        endMarker: "p",
                        endLineSep: "\r\n",
                        children: []
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "larop",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "u",
                        indent: "",
                        marker: "g:",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "gof",
                        endLineSep: "\n",
                        children: []
                    }
                ]
            };
            const parentNode: SourceCodeAst | NestedBlockAstNode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "",
                        lineSep: "\n"
                    },
                    childNode1,
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "j>",
                        markerAftermath: "",
                        indent: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "i",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "p",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "",
                        indent: "",
                        marker: "g8",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "p8",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "ef",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "cd",
                                lineSep: "\n"
                            }
                        ]
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "ooods",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "g:",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "js",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "a:",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "ooods",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "a8",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "j>",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "",
                        marker: "g:",
                        markerAftermath: "",
                        lineSep: "\n"
                    },
                ]
            };
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 1,
                nestedBlockUsed: true,
                argsExclEndIdxInParentNode: 2,
                endArgsExclEndIdxInParentNode: 3,
                children: [{
                    parentNode: childNode1,
                    idxInParentNode: 0,
                    nestedBlockUsed: false,
                    argsExclEndIdxInParentNode: 1,
                    endArgsExclEndIdxInParentNode: -1
                }, {
                    parentNode: childNode1,
                    idxInParentNode: 5,
                    nestedBlockUsed: false,
                    argsExclEndIdxInParentNode: 6,
                    endArgsExclEndIdxInParentNode: -1
                }]
            };
            let expected = [
                {
                    parentNode: childNode1,
                    idxInParentNode: 8,
                    nestedBlockUsed: false
                },
                {
                    parentNode: childNode1,
                    idxInParentNode: 10,
                    nestedBlockUsed: true
                },
                {
                    parentNode: parentNode,
                    idxInParentNode: 5,
                    nestedBlockUsed: true
                }
            ];
            const instance = new DefaultAstTransformerFormer();
            instance.augCodeMarkers = ["a8", "js"];
            instance.augCodeJsonArgMarkers = ["j>"];
            instance.augCodeArgSepMarkers = [];
            instance.augCodeArgMarkers = ["a:"];
            instance.genCodeMarkers = ["g8", "g:"];
            let actual = instance.extractGenCodeSections(augCode);
            assert.deepEqual(actual, expected);

            expected = [
                {
                    parentNode: childNode1,
                    idxInParentNode: 4,
                    nestedBlockUsed: false
                }
            ];
            actual = instance.extractGenCodeSections(augCode.children[0]);
            assert.deepEqual(actual, expected);

            expected = [
                {
                    parentNode: childNode1,
                    idxInParentNode: 7,
                    nestedBlockUsed: true
                }
            ];
            actual = instance.extractGenCodeSections(augCode.children[1]);
            assert.deepEqual(actual, expected);

            const augCodeWithoutGenCode = {
                parentNode: parentNode,
                idxInParentNode: 9,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 11,
                endArgsExclEndIdxInParentNode: -1
            } as any;
            expected = [];
            actual = instance.extractAugCodes(augCodeWithoutGenCode);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#_getLastGenCodeSection", function() {
        const parentNode = {
            type: AstBuilder.TYPE_NESTED_BLOCK,
            indent: "  ",
            marker: "beig",
            markerAftermath: "",
            lineSep: "\n",
            endIndent: "",
            endMarker: "es",
            endMarkerAftermath: "kop",
            endLineSep: "\r\n",
            children: [
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "beig",
                    markerAftermath: "0",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "c::",
                    lineSep: "\n"
                },
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "\t",
                    marker: "notation",
                    markerAftermath: "2",
                    lineSep: "\n"
                },
                {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "ad",
                    lineSep: "\n"
                },
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: " ",
                    marker: "[[!",
                    markerAftermath: "4",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: " ",
                    marker: "indu",
                    markerAftermath: "5",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "o",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "input_arg",
                    markerAftermath: "7",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "8",
                    indent: "",
                    marker: "{{",
                    lineSep: "\n",
                    endIndent: "\t",
                    endMarker: "p",
                    endLineSep: "\r\n",
                    children: []
                },
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "beig",
                    markerAftermath: "9",
                    lineSep: "\n"
                },
                {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "larop",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "\t\t",
                    marker: "sep,,",
                    markerAftermath: "11",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "12",
                    indent: "",
                    marker: "[[!",
                    lineSep: "\n",
                    endIndent: "",
                    endMarker: "gof",
                    endLineSep: "\n",
                    children: []
                },
                {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "indu",
                    markerAftermath: "13",
                    lineSep: "\n"
                },
                {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "larop",
                    lineSep: "\r\n"
                },
                {
                    type: AstBuilder.TYPE_NESTED_BLOCK,
                    markerAftermath: "15",
                    endMarkerAftermath: "v",
                    indent: "",
                    marker: "indu",
                    lineSep: "\n",
                    endIndent: "",
                    endMarker: "gof",
                    endLineSep: "\n",
                    children: [
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "o",
                            lineSep: "\r\n"
                        },
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            indent: " ",
                            marker: "indu",
                            markerAftermath: "1",
                            lineSep: "\r\n"
                        },
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "o",
                            lineSep: "\r\n"
                        },
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            indent: "",
                            marker: "{{",
                            markerAftermath: "3",
                            lineSep: "\r\n"
                        },
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "o",
                            lineSep: "\r\n"
                        }]
                },
                {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "16",
                    indent: "",
                    marker: "[[!",
                    lineSep: "\n",
                    endIndent: "",
                    endMarker: "gof",
                    endLineSep: "\n",
                    children: []
                },
            ]
        };
        const instance = new DefaultAstTransformerFormer();
        instance.augCodeMarkers = ["indu", "beig"];
        instance.augCodeJsonArgMarkers = ["notation"];
        instance.augCodeArgSepMarkers = ["sep,,"];
        instance.augCodeArgMarkers = ["input_arg"];
        instance.genCodeMarkers = ["[[!", "{{"];
        it("should pass with input 0", function() {
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 0,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 1,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = null;
            const actual = instance._getLastGenCodeSection(augCode);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 1", function() {
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 5,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 6,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = null;
            const actual = instance._getLastGenCodeSection(augCode);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 2", function() {
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 9,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 10,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = null;
            const actual = instance._getLastGenCodeSection(augCode);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 3", function() {
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 13,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 14,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected = null;
            const actual = instance._getLastGenCodeSection(augCode);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 4", function() {
            const augCode: any = {
                parentNode: parentNode,
                idxInParentNode: 15,
                nestedBlockUsed: true,
                argsExclEndIdxInParentNode: 0,
                endArgsExclEndIdxInParentNode: 16
            };
            const expected = {
                parentNode: parentNode,
                idxInParentNode: 16,
                nestedBlockUsed: true
            };
            const actual = instance._getLastGenCodeSection(augCode);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 5", function() {
            const augCode: any = {
                parentNode: parentNode.children[15],
                idxInParentNode: 1,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 2,
                endArgsExclEndIdxInParentNode: -1
            };
            const expected: any = {
                parentNode: parentNode.children[15],
                idxInParentNode: 3,
                nestedBlockUsed: false
            };
            const actual = instance._getLastGenCodeSection(augCode);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#_createGenCodeNode", function() {
        it("should pass with input 0", function() {
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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
            const instance = new DefaultAstTransformerFormer();
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

    describe("#computeAugCodeTransforms", function() {
        it("should pass for input 0", function() {
            const transformParentOfAugCodeNode = true;
            const targetNode: any = {
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-0"
                    }
                ]
            };
            const augCode: any = {
                parentNode: targetNode,
                idxInParentNode: 0,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 1,
                endArgsExclEndIdxInParentNode: -1
            };
            const genCodeSections = new Array<GeneratedCodeDescriptor>();
            const genCodes = new Array<GeneratedCodeSectionTransform | null>();
            const expected = new Array<DefaultAstTransformSpec>();
            const actual = new Array<DefaultAstTransformSpec>();
            DefaultAstTransformerFormer.computeAugCodeTransforms(augCode, transformParentOfAugCodeNode,
                genCodeSections, genCodes, actual);
            assert.deepEqual(actual, expected);
        });

        it("should pass for input 1", function() {
            const transformParentOfAugCodeNode = true;
            const targetNode: any = {
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-0"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-1"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-2"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-3"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-4"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-5"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-6"
                    }
                ]
            };
            const augCode: any = {
                parentNode: targetNode,
                idxInParentNode: 0,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 1,
                endArgsExclEndIdxInParentNode: -1
            };
            const genCodeSections: GeneratedCodeDescriptor[] = [
                {
                    parentNode: targetNode,
                    idxInParentNode: 1,
                    nestedBlockUsed: false
                },
                {
                    parentNode: targetNode,
                    idxInParentNode: 3,
                    nestedBlockUsed: false
                },
                {
                    parentNode: targetNode,
                    idxInParentNode: 4,
                    nestedBlockUsed: false
                },
                {
                    parentNode: targetNode,
                    idxInParentNode: 5,
                    nestedBlockUsed: false
                }
            ];
            const genCodes: Array<GeneratedCodeSectionTransform | null> = [
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: true // ensure that it is ignored
                },
                null,
                {
                    node: null,
                    ignore: true,
                    ignoreRemainder: false
                }
            ];
            const expected: DefaultAstTransformSpec[] = [
                {
                    node: targetNode,
                    childIndex: 1,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"                        
                    },
                    performDeletion: false,
                    childToInsert: null
                },
                {
                    node: targetNode,
                    childIndex: 3,
                    replacementChild: null,
                    performDeletion: true,
                    childToInsert: null
                },
                {
                    node: targetNode,
                    childIndex: 5,
                    replacementChild: null,
                    performDeletion: true,
                    childToInsert: null
                }
            ];
            const actual = new Array<DefaultAstTransformSpec>();
            DefaultAstTransformerFormer.computeAugCodeTransforms(augCode, transformParentOfAugCodeNode,
                genCodeSections, genCodes, actual);
            assert.deepEqual(actual, expected);
        });

        it("should pass for input 2", function() {
            const transformParentOfAugCodeNode = false;
            const targetNode: any = {
                type: AstBuilder.TYPE_NESTED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c0"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c1"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c2"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c3"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c4"
                    }
                ]
            };
            const augCode: any = {
                parentNode: {
                    children: [
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            marker: "deo-0"
                        },
                        targetNode,
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            marker: "deo-2"
                        },
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            marker: "deo-3"
                        }
                    ]
                },
                idxInParentNode: 1,
                nestedBlockUsed: true,
                argsExclEndIdxInParentNode: 0,
                endArgsExclEndIdxInParentNode: 3
            };
            const genCodeSections: any[] = [
                {
                    parentNode: targetNode,
                    idxInParentNode: 0,
                    nestedBlockUsed: false
                },
                {
                    parentNode: targetNode,
                    idxInParentNode: 1,
                    nestedBlockUsed: false
                },
                {
                    parentNode: targetNode,
                    idxInParentNode: 3,
                    nestedBlockUsed: false
                },
                {
                    parentNode: targetNode,
                    idxInParentNode: 4,
                    nestedBlockUsed: false
                }
            ];
            const genCodes: Array<GeneratedCodeSectionTransform | null> = [
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: false
                },
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "2",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: false
                },
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "3",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: true
                }
            ];
            const expected: DefaultAstTransformSpec[] = [
                {
                    node: targetNode,
                    childIndex: 0,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"                        
                    },
                    performDeletion: false,
                    childToInsert: null
                },
                {
                    node: targetNode,
                    childIndex: 1,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "2",
                        lineSep: "\n"                        
                    },
                    performDeletion: false,
                    childToInsert: null
                },
                {
                    node: targetNode,
                    childIndex: 3,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "3",
                        lineSep: "\n"                        
                    },
                    performDeletion: false,
                    childToInsert: null
                }
            ];
            const actual = new Array<DefaultAstTransformSpec>();
            DefaultAstTransformerFormer.computeAugCodeTransforms(augCode, transformParentOfAugCodeNode,
                genCodeSections, genCodes, actual);
            assert.deepEqual(actual, expected);
        });

        it("should pass for input 3", function() {
            const transformParentOfAugCodeNode = false;
            const targetNode: any = {
                type: AstBuilder.TYPE_NESTED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c0"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c1"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c2"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c3"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "c4"
                    }
                ]
            };
            const augCode: any = {
                parentNode: {
                    children: [
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            marker: "deo-0"
                        },
                        targetNode,
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            marker: "deo-2"
                        },
                        {
                            type: AstBuilder.TYPE_DECORATED_LINE,
                            marker: "deo-3"
                        }
                    ]
                },
                idxInParentNode: 1,
                nestedBlockUsed: true,
                argsExclEndIdxInParentNode: 0,
                endArgsExclEndIdxInParentNode: 3
            };
            const genCodeSections: any[] = [
                {
                    parentNode: targetNode,
                    idxInParentNode: 1,
                    nestedBlockUsed: false
                }
            ];
            const genCodes: Array<GeneratedCodeSectionTransform | null> = [
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: false
                },
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "2",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: false
                },
                null,
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "3",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: true
                },
                {
                    node: null,
                    ignore: true,
                    ignoreRemainder: false
                }
            ];
            const expected: DefaultAstTransformSpec[] = [
                {
                    node: targetNode,
                    childIndex: 1,
                    replacementChild: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"                        
                    },
                    performDeletion: false,
                    childToInsert: null
                },
                {
                    node: targetNode,
                    childIndex: 2,
                    replacementChild: null,
                    performDeletion: false,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "2",
                        lineSep: "\n"                        
                    }
                },
                {
                    node: targetNode,
                    childIndex: 2,
                    replacementChild: null,
                    performDeletion: false,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "3",
                        lineSep: "\n"                        
                    }
                }
            ];
            const actual = new Array<DefaultAstTransformSpec>();
            DefaultAstTransformerFormer.computeAugCodeTransforms(augCode, transformParentOfAugCodeNode,
                genCodeSections, genCodes, actual);
            assert.deepEqual(actual, expected);
        });

        it("should pass for input 4", function() {
            const transformParentOfAugCodeNode = true;
            const targetNode: any = {
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-0"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-1"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-2"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-3"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-4"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-5"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-6"
                    }
                ]
            };
            const augCode: any = {
                parentNode: targetNode,
                idxInParentNode: 0,
                nestedBlockUsed: false,
                argsExclEndIdxInParentNode: 3,
                endArgsExclEndIdxInParentNode: -1
            };
            const genCodeSections = new Array<GeneratedCodeDescriptor>();
            const genCodes: Array<GeneratedCodeSectionTransform | null> = [
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: true // ensure that it is ignored
                },
                null,
                {
                    node: null,
                    ignore: true,
                    ignoreRemainder: false
                }
            ];
            const expected: DefaultAstTransformSpec[] = [
                {
                    node: targetNode,
                    childIndex: 3,
                    replacementChild: null,
                    performDeletion: false,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"                        
                    }
                }
            ];
            const actual = new Array<DefaultAstTransformSpec>();
            DefaultAstTransformerFormer.computeAugCodeTransforms(augCode, transformParentOfAugCodeNode,
                genCodeSections, genCodes, actual);
            assert.deepEqual(actual, expected);
        });

        it("should pass for input 5", function() {
            const transformParentOfAugCodeNode = true;
            const targetNode: any = {
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-0"
                    },
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        children: [
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                marker: "c0"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                marker: "c1"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                marker: "c2"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                marker: "c3"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                marker: "c4"
                            }
                        ]
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-2"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        marker: "deo-3"
                    }
                ]
            };
            const augCode: any = {
                parentNode: targetNode,
                idxInParentNode: 1,
                nestedBlockUsed: true,
                argsExclEndIdxInParentNode: 1,
                endArgsExclEndIdxInParentNode: 3
            };
            const genCodeSections = new Array<GeneratedCodeDescriptor>();
            const genCodes: Array<GeneratedCodeSectionTransform | null> = [
                null,
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: false
                },
                {
                    node: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "2",
                        lineSep: "\n"
                    },
                    ignore: false,
                    ignoreRemainder: false
                }
            ];
            const expected: DefaultAstTransformSpec[] = [
                {
                    node: targetNode,
                    childIndex: 3,
                    replacementChild: null,
                    performDeletion: false,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "1",
                        lineSep: "\n"                        
                    }
                },
                {
                    node: targetNode,
                    childIndex: 3,
                    replacementChild: null,
                    performDeletion: false,
                    childToInsert: {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "2",
                        lineSep: "\n"                        
                    }
                }
            ];
            const actual = new Array<DefaultAstTransformSpec>();
            DefaultAstTransformerFormer.computeAugCodeTransforms(augCode, transformParentOfAugCodeNode,
                genCodeSections, genCodes, actual);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#_generateGenCodeTransforms", function() {
        it("should pass with input 0", function() {
            const instance = new DefaultAstTransformerFormer();
            instance.defaultGenCodeStartMarker = ":se:";
            instance.defaultGenCodeEndMarker = ":ee:";
            instance.defaultGenCodeInlineMarker = ":ise:";
            const genCodeSections: Array<GeneratedCodeDescriptor> = [
                {
                    parentNode: {
                        type: AstBuilder.TYPE_SOURCE_CODE,
                        children: [                            
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "dk",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "\t", 
                                marker: ":dl:",
                                markerAftermath: "ul",
                                lineSep: "\n"
                            }
                        ]
                    },
                    idxInParentNode: 1,
                    nestedBlockUsed: false
                }
            ];
            const genCodes: Array<GeneratedCode | null> = [
                {
                    contentParts: [
                        {
                            content: "te\ra",
                            exempt: false
                        }
                    ],
                    indent: " \t ",
                    useInlineMarker: false,
                    ignore: false,
                    ignoreRemainder: true,
                }
            ];
            const defaultIndent = " ";
            const defaultLineSep = "\r\n";
            const expected = [
                {
                    ignore: false,
                    ignoreRemainder: true,
                    node: {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "",
                        indent: "\t",
                        marker: ":se:",
                        lineSep: "\n",
                        endIndent: "\t",
                        endMarker: ":ee:",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: " \t te",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: " \t a",
                                lineSep: "\n"
                            }
                        ]
                    }
                }
            ];
            const actual = instance._generateGenCodeTransforms(
                defaultIndent, defaultLineSep, genCodes, genCodeSections);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 1", function() {
            const instance = new DefaultAstTransformerFormer();
            const genCodeSections: Array<GeneratedCodeDescriptor> = [
                {
                    parentNode: {
                        type: AstBuilder.TYPE_SOURCE_CODE,
                        children: [                            
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "dk",
                                lineSep: "\r\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "\t", 
                                marker: ":dl:",
                                markerAftermath: "ul",
                                lineSep: "\n"
                            }
                        ]
                    },
                    idxInParentNode: 1,
                    nestedBlockUsed: false
                }
            ];
            const genCodes: Array<GeneratedCode | null> = [
                {
                    contentParts: [
                        {
                            content: "tera",
                            exempt: false
                        }
                    ],
                    indent: " ",
                    useInlineMarker: true,
                    ignore: false,
                    ignoreRemainder: true,
                }
            ];
            const defaultIndent = "\t\t";
            const defaultLineSep = "\r\n";
            const expected = [
                {
                    ignore: false,
                    ignoreRemainder: true,
                    node: {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "\t",
                        marker: ":dl:",
                        markerAftermath: " tera",
                        lineSep: "\n",
                    }
                }
            ];
            const actual = instance._generateGenCodeTransforms(
                defaultIndent, defaultLineSep, genCodes, genCodeSections);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 2", function() {
            const instance = new DefaultAstTransformerFormer();
            const sourceCode = {
                type: AstBuilder.TYPE_NESTED_BLOCK,
                children: [
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "-09",
                        indent: "\t",
                        marker: ":dl:",
                        lineSep: "\n",
                        endIndent: "\t\t", 
                        endMarker: ":dul:",
                        endLineSep: "\r\n",
                        children: []
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "dk",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "\t", 
                        marker: ":i7e:",
                        markerAftermath: "ul",
                        lineSep: "\n"
                    },
                ]
            };
            // test ignoring of excess gen code sections.
            const genCodeSections: Array<GeneratedCodeDescriptor> = [
                {
                    parentNode: sourceCode,
                    idxInParentNode: 0,
                    nestedBlockUsed: true
                },
                {
                    parentNode: sourceCode,
                    idxInParentNode: 2,
                    nestedBlockUsed: false
                }
            ];
            const genCodes: Array<GeneratedCode | null> = [
                {
                    ignore: false,
                    ignoreRemainder: false,
                    contentParts: [
                        {
                            content: "the\r",
                            exempt: true
                        },
                        {
                            content: "te\r:dul:-09",
                            exempt: false
                        }
                    ],
                    indent: "  ",
                    useInlineMarker: false,
                }
            ];
            const defaultIndent = "";
            const defaultLineSep = "\r";
            const expected = [
                {
                    ignore: false,
                    ignoreRemainder: false,
                    node: {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "-09-1",
                        indent: "\t",
                        marker: ":dl:",
                        lineSep: "\n",
                        endIndent: "\t\t",
                        endMarker: ":dul:",
                        endLineSep: "\r\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "the",
                                lineSep: "\r"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "  te",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "  :dul:-09",
                                lineSep: "\n"
                            }
                        ]
                    }
                }
            ];
            const actual = instance._generateGenCodeTransforms(
                defaultIndent, defaultLineSep, genCodes, genCodeSections);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 3", function() {
            const instance = new DefaultAstTransformerFormer();
            instance.defaultGenCodeStartMarker = ":s1e:";
            instance.defaultGenCodeEndMarker = ":e2e:";
            instance.defaultGenCodeInlineMarker = ":i7e:";
            const sourceCode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "\t", 
                        marker: ":dl:",
                        markerAftermath: "ul",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "dk",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "\t", 
                        marker: ":i7e:",
                        markerAftermath: "ul",
                        lineSep: "\n"
                    },
                ]
            };
            const genCodeSections: Array<GeneratedCodeDescriptor> = [
                {
                    parentNode: sourceCode,
                    idxInParentNode: 0,
                    nestedBlockUsed: false
                },
                {
                    parentNode: sourceCode,
                    idxInParentNode: 2,
                    nestedBlockUsed: false
                }
            ];
            const genCodes: Array<GeneratedCode | null> = [
                null,
                {
                    ignore: true,
                    ignoreRemainder: false,
                    contentParts: null,
                    indent: null,
                    useInlineMarker: false,
                },
                null,
                {
                    ignore: true,
                    ignoreRemainder: true,
                    contentParts: [],
                    indent: "",
                    useInlineMarker: true,
                },
                {
                    ignore: false,
                    ignoreRemainder: false,
                    contentParts: [
                        {
                            content: "te\ra",
                            exempt: false
                        }
                    ],
                    indent: " \t ",
                    useInlineMarker: true,
                }
            ];
            const defaultIndent = " ";
            const defaultLineSep = "\r\n";
            const expected = [
                null,
                {
                    ignore: true,
                    ignoreRemainder: false,
                    node: null,
                },
                null,
                {
                    ignore: true,
                    ignoreRemainder: true,
                    node: null,
                },
                {
                    ignore: false,
                    ignoreRemainder: false,
                    node: {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: ":i7e:",
                        markerAftermath: " \t te",
                        lineSep: "\r\n",
                    }
                }
            ];
            const actual = instance._generateGenCodeTransforms(
                defaultIndent, defaultLineSep, genCodes, genCodeSections);
            assert.deepEqual(actual, expected);
        });

        it("should pass with input 4", function() {
            const instance = new DefaultAstTransformerFormer();
            instance.defaultGenCodeStartMarker = ":s1e:";
            instance.defaultGenCodeEndMarker = ":e2e:";
            instance.defaultGenCodeInlineMarker = ":i7e:";
            const sourceCode = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "\t", 
                        marker: ":dl:",
                        markerAftermath: "ul",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "dk",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: "\t", 
                        marker: ":i7e:",
                        markerAftermath: "ul",
                        lineSep: "\n"
                    },
                ]
            };
            const genCodeSections = new Array<GeneratedCodeDescriptor>();
            const genCodes: Array<GeneratedCode | null> = [
                {
                    ignore: false,
                    ignoreRemainder: false,
                    contentParts: [],
                    indent: "",
                    useInlineMarker: false,
                },
                {
                    ignore: false,
                    ignoreRemainder: false,
                    contentParts: [
                        {
                            content: "te\ra",
                            exempt: false
                        }
                    ],
                    indent: " \t ",
                    useInlineMarker: true,
                }
            ];
            const defaultIndent = " ";
            const defaultLineSep = "\r\n";
            const expected = [
                {
                    ignore: false,
                    ignoreRemainder: false,
                    node: {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        markerAftermath: "",
                        indent: " ",
                        marker: ":s1e:",
                        lineSep: "\r\n",
                        endIndent: " ",
                        endMarker: ":e2e:",
                        endLineSep: "\r\n",
                        children: []
                    },
                },
                {
                    ignore: false,
                    ignoreRemainder: false,
                    node: {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: ":i7e:",
                        markerAftermath: " \t te",
                        lineSep: "\r\n",
                    }
                }
            ];
            const actual = instance._generateGenCodeTransforms(
                defaultIndent, defaultLineSep, genCodes, genCodeSections);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#performTransformations", function() {
        it("should pass with empty input", function() {
            DefaultAstTransformerFormer.performTransformations([]);
        });
        it("should pass with no additions", function() {
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
            DefaultAstTransformerFormer.performTransformations(transformSpecs);
            assert.deepEqual(node1, expected);
        });
        it("should pass with some additions", function() {
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
            DefaultAstTransformerFormer.performTransformations(transformSpecs);
            assert.deepEqual(node1, expected1);
            assert.deepEqual(node2, expected2);
        });
    });
});
