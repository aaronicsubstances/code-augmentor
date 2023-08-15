import { assert } from "chai";

import { AstParser } from "../src/AstParser";
import { AstFormatter } from "../src/AstFormatter";
import { DecoratedLineAstNode, SourceCodeAst, UndecoratedLineAstNode } from "../src/types";

describe('AstFormatter', function() {

    describe('#stringify', function() {
        it(`should pass with input 0`, function() {
            const ast = {
                type: AstParser.TYPE_SOURCE_CODE
            };
            const expected = "";
            const actual = AstFormatter.stringify(ast as SourceCodeAst);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 1`, function() {
            const ast = {
                type: AstParser.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstParser.TYPE_UNDECORATED_LINE,
                        text: "abc",
                        lineSep: "\n"
                    },
                    {
                        type: AstParser.TYPE_UNDECORATED_LINE,
                        text: "de",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstParser.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "d:",
                        markerAftermath: "oods",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstParser.TYPE_ESCAPED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n",
                    },
                    {
                        type: AstParser.TYPE_ESCAPED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "1",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            }
                        ]
                    },
                    {
                        type: AstParser.TYPE_NESTED_BLOCK,
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
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "1",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "d:",
                                markerAftermath: "12",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: " ",
                                marker: "2d:",
                                markerAftermath: "32",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "  ",
                                marker: "d:",
                                markerAftermath: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "   ",
                                marker: "d:",
                                markerAftermath: "5",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "    ",
                                marker: "2d:",
                                markerAftermath: "6",
                                lineSep: "\n"
                            }
                        ]
                    }
                ]
            };
            const expected =
                "abc\n" +
                "de\r\n" +
                " d:oods\r\n" +
                " g:oods\n" +
                "k:oods\n" +
                " g:oods\n" +
                "0\n" +
                "1\n" +
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
                "    2d:6\n" +
                " k-age\n";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 2`, function() {
            const ast = {
                type: AstParser.TYPE_SOURCE_CODE,
                children: [
                    {
                        type: AstParser.TYPE_UNDECORATED_LINE,
                        text: "abc",
                        lineSep: "\n"
                    },
                    {
                        type: AstParser.TYPE_UNDECORATED_LINE,
                        text: "de",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstParser.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "d:",
                        markerAftermath: "oods",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstParser.TYPE_NESTED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n"
                    },
                    {
                        type: AstParser.TYPE_NESTED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n",
                        endMarkerAftermath: "oops",
                        children: [
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "1",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            }
                        ]
                    },
                    {
                        type: AstParser.TYPE_ESCAPED_BLOCK,
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
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "1",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "d:",
                                markerAftermath: "12",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: " ",
                                marker: "2d:",
                                markerAftermath: "32",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "  ",
                                marker: "d:",
                                markerAftermath: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "   ",
                                marker: "d:",
                                markerAftermath: "5",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "    ",
                                marker: "2d:",
                                markerAftermath: "6",
                                lineSep: "\n"
                            }
                        ]
                    }
                ]
            };
            const expected =
                "abc\n" +
                "de\r\n" +
                " d:oods\r\n" +
                " g:oods\n" +
                "k:\n" +
                " g:oods\n" +
                "0\n" +
                "1\n" +
                "2\n" +
                "3\n" +
                "4\n" +
                "k:oops\n" +
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
                "    2d:6\n" +
                " k-oes\n";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 3`, function() {
            const ast = {
                type: AstParser.TYPE_UNDECORATED_LINE
            };
            const expected = "";
            const actual = AstFormatter.stringify(ast as UndecoratedLineAstNode);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 4`, function() {
            const ast = {
                type: AstParser.TYPE_NESTED_BLOCK,
                lineSep: "\r\n",
                endLineSep: "\r",
                indent: "  ",
                endIndenet: " ",
                marker: "s->",
                markerAftermath: ">",
                endMarker: "e->",
                endMarkerAftermath: "",
                children: [
                    {
                        type: AstParser.TYPE_UNDECORATED_LINE,
                        text: "abc",
                        lineSep: "\n"
                    },
                    {
                        type: AstParser.TYPE_UNDECORATED_LINE,
                        text: "de",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstParser.TYPE_DECORATED_LINE,
                        indent: " ",
                        marker: "d:",
                        markerAftermath: "oods",
                        lineSep: "\r\n"
                    },
                    {
                        type: AstParser.TYPE_ESCAPED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n",
                    },
                    {
                        type: AstParser.TYPE_ESCAPED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n",
                        children: [
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "1",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            }
                        ]
                    },
                    {
                        type: AstParser.TYPE_NESTED_BLOCK,
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
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "0",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "1",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "2",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "3",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_UNDECORATED_LINE,
                                text: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "",
                                marker: "d:",
                                markerAftermath: "12",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: " ",
                                marker: "2d:",
                                markerAftermath: "32",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "  ",
                                marker: "d:",
                                markerAftermath: "4",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "   ",
                                marker: "d:",
                                markerAftermath: "5",
                                lineSep: "\n"
                            },
                            {
                                type: AstParser.TYPE_DECORATED_LINE,
                                indent: "    ",
                                marker: "2d:",
                                markerAftermath: "6",
                                lineSep: "\n"
                            }
                        ]
                    }
                ]
            };
            const expected =
                "  s->>\r\n" +
                "abc\n" +
                "de\r\n" +
                " d:oods\r\n" +
                " g:oods\n" +
                "k:oods\n" +
                " g:oods\n" +
                "0\n" +
                "1\n" +
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
                "    2d:6\n" +
                " k-age\n" +
                "e->\r";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 5`, function() {
            const ast: DecoratedLineAstNode = {
                type: AstParser.TYPE_DECORATED_LINE,
                indent: " ",
                marker: "m",
                markerAftermath: ":",
                lineSep: "\n"
            };
            const expected = " m:\n";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 6`, function() {
            const ast: DecoratedLineAstNode = {
                type: AstParser.TYPE_NESTED_BLOCK_START,
                indent: " ",
                marker: "m",
                markerAftermath: ":",
                lineSep: null as any
            };
            const expected = " m:";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 7`, function() {
            const ast: DecoratedLineAstNode = {
                type: AstParser.TYPE_NESTED_BLOCK_END,
                indent: "c", // test that invalid is simply printed
                marker: "m",
                markerAftermath: ":",
                lineSep: "p"
            };
            const expected = "cm:p";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 8`, function() {
            const ast: DecoratedLineAstNode = {
                type: AstParser.TYPE_ESCAPED_BLOCK_START,
                indent: " ",
                marker: "m",
                markerAftermath: ":",
                lineSep: "\r\n"
            };
            const expected = " m:\r\n";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 9`, function() {
            const ast: DecoratedLineAstNode = {
                type: AstParser.TYPE_ESCAPED_BLOCK_END,
                indent: "c", // test that invalid is simply printed
                marker: "x",
                markerAftermath: ":",
                lineSep: "p"
            };
            const expected = "cx:p";
            const actual = AstFormatter.stringify(ast);
            assert.deepEqual(actual, expected);
        });
        it('should fail with input 0', function() {
            assert.throws(function() {
                AstFormatter.stringify(null as any);
            });
        });
        it('should fail with input 1', function() {
            assert.throws(function() {
                AstFormatter.stringify({} as any);
            }, "unexpected node type");
        });
        it('should fail with input 2', function() {
            assert.throws(function() {
                AstFormatter.stringify({
                    type: AstParser.TYPE_SOURCE_CODE,
                    children: [
                        {
                            type: AstParser.TYPE_NESTED_BLOCK,
                            children: [{
                                type: "1268-dabb"
                            }]
                        }
                    ]
                } as any);
            }, "1268-dabb");
        });
    });
});
