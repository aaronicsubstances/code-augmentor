import { assert } from "chai";

import { AstBuilder } from "../src/AstBuilder";
import { AstFormatter } from "../src/AstFormatter";
import { SourceCodeAst, UndecoratedLineAstNode } from "../src/types";

describe('AstFormatter', function() {

    describe('#stringify', function() {
        it(`should pass with input 0`, function() {
            const ast = {
                type: AstBuilder.TYPE_SOURCE_CODE
            };
            const expected = "";
            const actual = AstFormatter.stringify(ast as SourceCodeAst);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 1`, function() {
            const ast = {
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
                        type: AstBuilder.TYPE_NESTED_BLOCK,
                        indent: " ",
                        marker: "g:",
                        markerAftermath: "oods",
                        lineSep: "\n",
                        endIndent: "",
                        endMarker: "k:",
                        endLineSep: "\n"
                    },
                    {
                        type: AstBuilder.TYPE_NESTED_BLOCK,
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
                type: AstBuilder.TYPE_UNDECORATED_LINE
            };
            const expected = "";
            const actual = AstFormatter.stringify(ast as UndecoratedLineAstNode);
            assert.deepEqual(actual, expected);
        });
        it(`should pass with input 4`, function() {
            const ast = {
                type: AstBuilder.TYPE_NESTED_BLOCK,
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
                    type: AstBuilder.TYPE_SOURCE_CODE,
                    children: [
                        {
                            type: AstBuilder.TYPE_NESTED_BLOCK,
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
