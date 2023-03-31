import os from "os";

import { assert } from "chai";

import AstBuilder from "../src/AstBuilder";
import { SourceCodeAst } from "../src/types";

describe('AstBuilder', function() {

    describe('#isMarkerSuitable', function() {
        let data = [
            { marker: "", expected: false },
            { marker: "\t", expected: false },
            { marker: "\n", expected: false },
            { marker: "abc", expected: true },
            { marker: "a", expected: true },
            { marker: "abc\rd", expected: false },
            { marker: "abc\r\n", expected: false },
            { marker: "ab\tc n", expected: true }
        ];
        data.forEach(({ marker, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = AstBuilder.isMarkerSuitable(marker);
                assert.equal(actual, expected);
            });
        });
    });

    describe('#_findMarkerMatch', function() {
        let data = [
            { markers: [""], n: {text: '', indent: ''}, expected: null },
            { markers: ["ab", "", "c"], n: {text: '', indent: ''}, expected: null },
            { markers: ["ab", "", " ", "d\n", "   d"], n: {text: '   d', indent: '   '}, expected: null },
            { markers: ["so", "som"], n: {text: 'some', indent: ''}, expected: ["som", "e"] },
            { markers: ["abc"], n: {text: ' abc', indent: ' '}, expected: ["abc", ""] },
            { markers: ["ab", "", "c"], n: {text: '\t\tcanoe', indent: '\t\t'}, expected: ["c", "anoe"] },
            { markers: ["abc", "ab", "a"], n: {text: '.abcd', indent: ''}, expected: null },
            { markers: ["abc", "ab", "a"], n: {text: 'abcd', indent: ''}, expected: ["abc", "d"] }
        ];
        data.forEach(({ markers, n, expected }, i) => {
            it(`should pass with match details requested, and with input ${i}`, function() {
                const actual = AstBuilder._findMarkerMatch(markers, n);
                assert.deepEqual(actual, expected);
            });
        });
        data = [
            { markers: [""], n: {text: '', indent: ''}, expected: null },
            { markers: ["ab", "", "c"], n: {text: '', indent: ''}, expected: null },
            { markers: ["ab", "", " ", "d\n", "   d"], n: {text: '   d', indent: '   '}, expected: null },
            { markers: ["so", "som"], n: {text: 'some', indent: ''}, expected: [] },
            { markers: ["abc"], n: {text: ' abc', indent: ' '}, expected: [] },
            { markers: ["ab", "", "c"], n: {text: '\t\tcanoe', indent: '\t\t'}, expected: [] },
            { markers: ["abc", "ab", "a"], n: {text: '.abcd', indent: ''}, expected: null },
            { markers: ["abc", "ab", "a"], n: {text: 'abcd', indent: ''}, expected: [] }
        ];
        data.forEach(({ markers, n, expected }, i) => {
            it(`should pass with match details ignored, and with input ${i}`, function() {
                const actual = AstBuilder._findMarkerMatch(markers, n, true);
                assert.deepEqual(actual, expected);
            });
        });
    });

    describe('#parse', function() {
        it(`should pass with input 0`, function() {
            const s = "";
            const expected = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: []
            };
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d:", "2d:"];
            instance.escapedBlockStartMarkers = ["g:"];
            instance.escapedBlockEndMarkers = ["k:"];
            instance.nestedBlockStartMarkers = ["d-"];
            instance.nestedBlockEndMarkers = ["k-"];
            const actual = instance.parse(s);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 1`, function() {
            const s = "d:abc\r" +
                " k:de\r\n" +
                " d-ed f \n" +
                "  k-hghi";
            const expected = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "d:abc",
                        lineSep: "\r"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: " k:de",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: " d-ed f ",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "  k-hghi",
                        lineSep: ""
                    }
                ]
            };
            const instance = new AstBuilder();
            const actual = instance.parse(s);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 2`, function() {
            const s =
                "abc\n" +
                "de\r\n" +
                " d:oods\r\n" +
                " g:oods\n" +
                "k:oods\n" +
                " g:oods\n" +
                "g:0\n" +
                "k:1\n" +
                "2\n" +
                "3\n" +
                "4\n" +
                "k:oods\n" +
                "d-oes\r\n" +
                "0\n" +
                "1\n" +
                "2\n" +
                "3\n" +
                "4\n" +
                "d:12\n" +
                " 2d:32\n" +
                "  d:4\n" +
                "   d:5\n" +
                " d-\n" +
                " :2d\r\n" +
                "\tk-ds\n" +
                "    2d:6\n" +
                " k-age\n";
            const expected: SourceCodeAst = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "abc",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "de",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "d:",
                        markerAftermath: "oods",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n",
                        children: [],
                    },
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "g:0",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "k:1",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            }
                        ]
                    },
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        indent: "",
                        marker: "d-",
                        markerAftermath: "oes",
                        lineSep: "\r\n",
                        endIndent: " ",
                        endMarker: "k-",
                        endMarkerAftermath: "age",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
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
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "d:",
                                markerAftermath: "12",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: " ",
                                marker: "2d:",
                                markerAftermath: "32",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "  ",
                                marker: "d:",
                                markerAftermath: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "   ",
                                marker: "d:",
                                markerAftermath: "5",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_NESTED_BLOCK,
                                indent: " ",
                                marker: "d-",
                                markerAftermath: "",
                                lineSep: "\n",
                                endIndent: "\t",
                                endMarker: "k-",
                                endLineSep: "\n",
                                endMarkerAftermath: "ds",
                                children: [{
                                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                                    text: " :2d",
                                    lineSep: "\r\n"
                                }]
                            },
                            {
                                type: AstBuilder.TYPE_DECORATED_LINE,
                                indent: "    ",
                                marker: "2d:",
                                markerAftermath: "6",
                                lineSep: "\n"
                            }
                        ]
                    }
                ]
            };
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d:", "2d:"];
            instance.escapedBlockStartMarkers = ["g:"];
            instance.escapedBlockEndMarkers = ["k:"];
            instance.nestedBlockStartMarkers = ["d-"];
            instance.nestedBlockEndMarkers = ["k-"];
            const actual = instance.parse(s);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 3`, function() {
            const s =
                "abc\n" +
                "de\r\n" +
                " d:oods\r\n" +
                " (-ds\n" +
                "5k-)\n" +
                " (:oods\n" +
                "0\n" +
                "1\n" +
                "2\n" +
                "3\n" +
                "4\n" +
                ":)age\n" +
                "<.oes\r\n" +
                "0\n" +
                "1\n" +
                "2\n" +
                "3\n" +
                "4\n" +
                "d:12\n" +
                " 2d:32\n" +
                "  d:4\n" +
                "   d:5\n" +
                "    2d:6\n" +
                " >.oes\n";
            const expected = {
                type: AstBuilder.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "abc",
                        lineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: "de",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_UNDECORATED_LINE,
                        text: " d:oods",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        indent: " ",
                        marker: "(-",
                        markerAftermath: "ds",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "5k-)",
                        endLineSep: "\n",
                        endMarkerAftermath: "",
                        children: []
                    },
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        indent: " ",
                        marker: "(:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: ":)",
                        endLineSep: "\n",
                        endMarkerAftermath: "age",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
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
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            }
                        ]
                    },
                    {
                        type: AstBuilder.TYPE_ESCAPED_BLOCK,
                        indent: "",
                        marker: "<.",
                        markerAftermath: "oes",
                        lineSep: "\r\n",
                        endIndent: " ",
                        endMarker: ">.",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
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
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "d:12",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: " 2d:32",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "  d:4",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "   d:5",
                                lineSep: "\n"
                            },
                            {
                                type: AstBuilder.TYPE_UNDECORATED_LINE,
                                text: "    2d:6",
                                lineSep: "\n"
                            }
                        ]
                    }
                ]
            };
            const instance = new AstBuilder();
            instance.escapedBlockStartMarkers = ["<."];
            instance.escapedBlockEndMarkers = [">."];
            instance.nestedBlockStartMarkers = ["(-", "(:"];
            instance.nestedBlockEndMarkers = ["5k-)", ":)"];
            const actual = instance.parse(s);
            assert.deepEqual(actual, expected);
        });
    });

    describe("#escapeText", function() {
        it("should pass with input 0", function() {
            const instance = new AstBuilder();
            const s = "x\n g+u\r\n";
            const attrs = {};
            const forceEscapedNodeCreation = false;
            const expected = [{
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: "x",
                lineSep: "\n"
            }, {
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: " g+u",
                lineSep: "\r\n"
            }];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 1", function() {
            const instance = new AstBuilder();
            const s = "x\n g+u\r\n";
            const attrs = {
                marker: "g_s:",
                endMarker: "g_e:"
            };
            const forceEscapedNodeCreation = true;
            const expected: any = [{
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: "",
                indent: undefined,
                endIndent: undefined,
                marker: "g_s:",
                endMarker: "g_e:",
                lineSep: os.EOL,
                endLineSep: os.EOL,
                children: [{
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "x",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: " g+u",
                    lineSep: "\r\n"
                }]
            }];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 2", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["g6b:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "\tg6b:87 is yours \n x\n g+u\r\n";
            const attrs = {
                markerAftermath: "87",
                marker: "g6a:",
                endMarker: "g6b:",
                indent: " ",
                endIndent: "\t",
                lineSep: "\r\n",
                endLineSep: "\r"
            };
            const forceEscapedNodeCreation = false;
            const expected: any = [{
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: "87-1",
                indent: " ",
                endIndent: "\t",
                marker: "g6a:",
                endMarker: "g6b:",
                lineSep: "\r\n",
                endLineSep: "\r",
                children: [{
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "\tg6b:87 is yours ",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: " x",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: " g+u",
                    lineSep: "\r\n"
                }]
            }];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 3", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["g6b:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "wei<: is yours \nx\ng+u\r\n";
            const attrs = {
                markerAftermath: ":",
                marker: "a:",
                endMarker: "fg->",
                indent: "",
                endIndent: "",
                lineSep: "\n",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            const expected: any = [{
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: ":",
                indent: "",
                endIndent: "",
                marker: "a:",
                endMarker: "fg->",
                lineSep: "\n",
                endLineSep: "\n",
                children: [{
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "wei<: is yours ",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "x",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "g+u",
                    lineSep: "\r\n"
                }]
            }];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 4", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \nx\ng+u\r\n";
            const attrs = {
                markerAftermath: " is yours ",
                marker: "ha:",
                endMarker: "fg->",
                indent: "",
                endIndent: "",
                lineSep: "",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = true;
            const expected: any = [{
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: " is yours -1",
                indent: "",
                endIndent: "",
                marker: "ha:",
                endMarker: "fg->",
                lineSep: os.EOL,
                endLineSep: "\n",
                children: [{
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "fg-> is yours ",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "x",
                    lineSep: "\n"
                }, {
                    type: AstBuilder.TYPE_UNDECORATED_LINE,
                    text: "g+u",
                    lineSep: "\r\n"
                }]
            }];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 5", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \nafei\ng+u\r\n";
            const attrs = {
                indent: "c",
                endIndent: "c",
                lineSep: "dS",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            const expected: any = [{
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: "fg-> is yours ",
                lineSep: "\n"
            }, {
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: "afei",
                lineSep: "\n"
            }, {
                type: AstBuilder.TYPE_UNDECORATED_LINE,
                text: "g+u",
                lineSep: "\r\n"
            }];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 6", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "";
            const attrs = {
                indent: "c",
                endIndent: "c",
                lineSep: "dS",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            const expected: any = [];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should pass with input 7", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "";
            const attrs = {
                marker: "c",
                endMarker: "d",
                indent: "",
                endIndent: " "
            };
            const forceEscapedNodeCreation = true;
            const expected: any = [{
                type: AstBuilder.TYPE_ESCAPED_BLOCK,
                markerAftermath: "",
                marker: "c",
                endMarker: "d",
                indent: "",
                endIndent: " ",
                lineSep: os.EOL,
                endLineSep: os.EOL,
                children: []
            }];
            const actual = instance.escapeText(s, attrs,
                forceEscapedNodeCreation);
            assert.deepEqual(actual, expected);
        });
        it("should fail with invalid marker", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \nafei>\ng+u\r\n";
            const attrs = {
                marker: "x\n",
                endMarker: "y",
                indent: "\t",
                endIndent: "",
                lineSep: "\n",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            assert.throws(() => instance.escapeText(s, attrs,
                forceEscapedNodeCreation));
        });
        it("should fail with invalid end marker", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \nwei<\ng+u\r\n";
            const attrs = {
                marker: "xn",
                endMarker: " y",
                indent: "\t",
                endIndent: "",
                lineSep: "\n",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            assert.throws(() => instance.escapeText(s, attrs,
                forceEscapedNodeCreation));
        });
        it("should fail with invalid indent", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \nwei<\ng+u\r\n";
            const attrs = {
                marker: "xn",
                endMarker: "s y",
                indent: "\tx",
                endIndent: "",
                lineSep: "\n",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            assert.throws(() => instance.escapeText(s, attrs,
                forceEscapedNodeCreation));
        });
        it("should fail with invalid end indent", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \nd12>\ng+u\r\n";
            const attrs = {
                marker: "xn",
                endMarker: "s y",
                indent: "",
                endIndent: "u",
                lineSep: "\n",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            assert.throws(() => instance.escapeText(s, attrs,
                forceEscapedNodeCreation));
        });
        it("should fail with invalid line sep", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \nd6:>\ng+u\r\n";
            const attrs = {
                marker: "xn",
                endMarker: "s y",
                indent: "",
                endIndent: "",
                lineSep: "jn",
                endLineSep: "\n"
            };
            const forceEscapedNodeCreation = false;
            assert.throws(() => instance.escapeText(s, attrs,
                forceEscapedNodeCreation));
        });
        it("should fail with invalid end line sep", function() {
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d6:", "d12>"]
            instance.escapedBlockStartMarkers = [ "g6:", null, "", "g6a:"] as any[];
            instance.escapedBlockEndMarkers = ["end:"];
            instance.nestedBlockStartMarkers = ["wei<"];
            instance.nestedBlockEndMarkers = ["afei>"];
            const s = "fg-> is yours \ng6:>\ng+u\r\n";
            const attrs = {
                marker: "xn",
                endMarker: "s y",
                indent: "",
                endIndent: "",
                lineSep: "",
                endLineSep: "kn"
            };
            const forceEscapedNodeCreation = false;
            assert.throws(() => instance.escapeText(s, attrs,
                forceEscapedNodeCreation));
        });
    });

    describe("#createDecoratedLineNode", function() {
        const data = [
            {
                line: "",
                attrs: {
                    indent: "",
                    marker: "ode",
                    lineSep: "\n"
                },
                expected: {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "",
                    marker: "ode",
                    markerAftermath: "",
                    lineSep: "\n"
                }
            },
            {
                line: "xor",
                attrs: {
                    indent: "  ",
                    marker: "deo",
                    lineSep: "\r\n"
                },
                expected: {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: "  ",
                    marker: "deo",
                    markerAftermath: "xor",
                    lineSep: "\r\n"
                }
            },
            {
                line: '',
                attrs: {
                    indent: null,
                    marker: "e",
                    lineSep: "\r"
                },
                expected: {
                    type: AstBuilder.TYPE_DECORATED_LINE,
                    indent: null,
                    marker: "e",
                    markerAftermath: '',
                    lineSep: "\r"
                }
            },
        ];
        data.forEach(({ line, attrs, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = AstBuilder.createDecoratedLineNode(line, attrs);
                assert.deepEqual(actual, expected);
            });
        });
        const errorData = [
            {
                line: "1.",
                attrs: null,
                expected: "unsuitable marker"
            },
            {
                line: "",
                attrs: {
                    indent: "u",
                    marker: "ode",
                    lineSep: "\n"
                },
                expected: "non-blank indent"
            },
            {
                line: "xor",
                attrs: {
                    indent: "  ",
                    marker: "de\no",
                    lineSep: "\r\n"
                },
                expected: "unsuitable marker"
            },
            {
                line: '',
                attrs: {
                    indent: null,
                    marker: "e",
                    lineSep: "xxxxx"
                },
                expected: "xxxxx"
            },
            {
                line: '',
                attrs: {
                    indent: null,
                    marker: "e",
                    lineSep: null
                },
                expected: "invalid lineSep"
            }
        ];
        errorData.forEach(({ line, attrs, expected }, i) => {
            it(`should fail with input ${i}`, function() {
                assert.throws(function() {
                    AstBuilder.createDecoratedLineNode(line, attrs)
                }, expected);
            });
        });
    });

    describe("#createEscapedNode", function() {
        const data = [
            {
                lines: ["c", "\r\n"],
                attrs: {
                    indent: "   ",
                    marker: "ab",
                    lineSep: "\n",
                    endIndent: "  ",
                    endMarker: "de",
                    endLineSep: "\n",
                },
                expected: {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "",
                    indent: "   ",
                    marker: "ab",
                    lineSep: "\n",
                    endIndent: "  ",
                    endMarker: "de",
                    endLineSep: "\n",
                    children: [
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "c",
                            lineSep: "\r\n"
                        }
                    ]
                }
            },
            {
                lines: [" socks ", "\n", "j p sav", ""],
                attrs: {
                    markerAftermath: "shoe",
                    indent: " \t ",
                    marker: "the ",
                    lineSep: "\r",
                    endIndent: "",
                    endMarker: "your\t",
                    endLineSep: "\r\n"
                },
                expected: {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "shoe",
                    indent: " \t ",
                    marker: "the ",
                    lineSep: "\r",
                    endIndent: "",
                    endMarker: "your\t",
                    endLineSep: "\r\n",
                    children: [
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: " socks ",
                            lineSep: "\n"
                        },
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "j p sav",
                            lineSep: "\r"
                        }
                    ]
                }
            },
            {
                lines: [],
                attrs: {
                    markerAftermath: "tea",
                    indent: null,
                    marker: "e",
                    lineSep: "\r",
                    endIndent: null,
                    endMarker: "e",
                    endLineSep: "\r\n",
                },
                expected: {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "tea",
                    indent: null,
                    marker: "e",
                    lineSep: "\r",
                    endIndent: null,
                    endMarker: "e",
                    endLineSep: "\r\n",
                    children: []
                }
            },
            {
                lines: [
                    "no", "\n", "yes", "\n",
                    "e:tea cup", "\r\n", "\tsunshine", ""
                ],
                attrs: {
                    markerAftermath: "tea",
                    indent: " ",
                    marker: "s:",
                    lineSep: "\r",
                    endIndent: "  ",
                    endMarker: "e:",
                    endLineSep: "\r\n",
                },
                expected: {
                    type: AstBuilder.TYPE_ESCAPED_BLOCK,
                    markerAftermath: "tea-1",
                    indent: " ",
                    marker: "s:",
                    lineSep: "\r",
                    endIndent: "  ",
                    endMarker: "e:",
                    endLineSep: "\r\n",
                    children: [
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "no",
                            lineSep: "\n"
                        },
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "yes",
                            lineSep: "\n"
                        },
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "e:tea cup",
                            lineSep: "\r\n"
                        },
                        {
                            type: AstBuilder.TYPE_UNDECORATED_LINE,
                            text: "\tsunshine",
                            lineSep: "\r"
                        }
                    ]
                }
            }
        ];
        data.forEach(({ lines, attrs, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = AstBuilder.createEscapedNode(lines, attrs);
                assert.deepEqual(actual, expected);
            });
        });
        const errorData = [
            {
                lines: ["1.", "\n"],
                attrs: null,
                expected: "unsuitable start marker"
            },
            {
                lines: ["", ""],
                attrs: {
                    indent: "u",
                    marker: "ode",
                    lineSep: "\n",
                    endIndent: "",
                    endMarker: "ode",
                    endLineSep: "\n",
                },
                expected: "non-blank indent"
            },
            {
                lines: ["xor", "\n", "dd", "\n"],
                attrs: {
                    indent: "",
                    marker: "d\n",
                    lineSep: "\n",
                    endIndent: "",
                    endMarker: "ode",
                    endLineSep: "\n",
                },
                expected: "unsuitable start marker"
            },
            {
                lines: ["xor", "\n", "dd", "\n"],
                attrs: {
                    indent: "",
                    marker: "d",
                    lineSep: "\n",
                    endIndent: " ",
                    endMarker: "\tode",
                    endLineSep: "\n",
                },
                expected: "unsuitable end marker"
            },
            {
                lines: ['', "\r\n"],
                attrs: {
                    indent: "",
                    marker: "d",
                    lineSep: "xxxxx",
                    endIndent: " ",
                    endMarker: "ode",
                    endLineSep: "\n",
                },
                expected: "invalid lineSep: xxxxx"
            },
            {
                lines: ['', ''],
                attrs: {
                    indent: "",
                    marker: "d",
                    lineSep: "\n",
                    endIndent: "u",
                    endMarker: "ode",
                    endLineSep: "n",
                },
                expected: "non-blank end indent"
            },
            {
                lines: ['', ''],
                attrs: {
                    indent: "",
                    marker: "d",
                    lineSep: "\n",
                    endIndent: "\t",
                    endMarker: "ode",
                    endLineSep: "n",
                },
                expected: "invalid end lineSep"
            }
        ];
        errorData.forEach(({ lines, attrs, expected }, i) => {
            it(`should fail with input ${i}`, function() {
                assert.throws(function() {
                    AstBuilder.createEscapedNode(lines, attrs)
                }, expected);
            });
        });
    });
});
