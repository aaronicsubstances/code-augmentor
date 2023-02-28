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
            it(`should pass with input ${i}`, function() {
                const actual = AstBuilder._findMarkerMatch(markers, n);
                assert.deepEqual(actual, expected);
            });
        });
    });

    describe('#parse', function() {
        it(`should pass with input 0`, function() {
            const s =
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
            const instance = new AstBuilder();
            instance.decoratedLineMarkers = ["d:", "2d:"];
            instance.escapedBlockStartMarkers = ["g:"];
            instance.escapedBlockEndMarkers = ["k:"];
            instance.nestedBlockStartMarkers = ["d-"];
            instance.nestedBlockEndMarkers = ["k-"];
            const actual = instance.parse(s);
            assert.deepEqual(actual, expected);
        });
    });
});
