import { assert } from "chai";
import AstBuilder from "../src/AstBuilder";

import DefaultAstTransformer from "../src/DefaultAstTransformer";
import { DefaultAstTransformSpec, GeneratedCodeDescriptor, GeneratedCodePart } from "../src/types";

describe("DefaultAstTransformer", function() {

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
            const genCodeLines: string[] = [
                "in", "\n",
                " the ", "\r\n",
                "beginning", "\r\n"
            ];
            const useInlineMarker: boolean = false;
            const genCodeSection: GeneratedCodeDescriptor | null = null;
            const defaultIndent: string | null = "\t";
            const defaultLineSep: string | null = "\r";
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
