import { assert } from "chai";
import AstBuilder from "../src/AstBuilder";

import AstFormatter from "../src/AstFormatter";

describe('AstFormatter', function() {

    describe('#stringify', function() {
        it(`should pass with input 0`, function() {
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
    });
});
