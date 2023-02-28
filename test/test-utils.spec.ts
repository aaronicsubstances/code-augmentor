import { assert } from "chai";

import * as myutils from "../src/utils";

describe('utils', function(){
    describe('#printNormalDiff', function() {
        it('should convert successfully', function() {
            const x = [""];
            const y = [""];
            const expected = "";
            const actual = myutils.printNormalDiff(x, y);
            assert.deepEqual(actual, expected);
        });
    });

    describe('#splitIntoLines', function() {
        const data = [
            { text: "", expected: [] },
            { text: "\n", expected: ["", "\n"] },
            { text: "abc", expected: ["abc", ""] },
            { text: "ab\nc", expected: ["ab", "\n", "c", ""] },
            { text: "ab\nc\r\n", expected: ["ab", "\n", "c", "\r\n"]}
        ];
        data.forEach(({ text, expected }, i) => {
            it(`should test splitIntoLines correctly - ${i}`, function() {
                const actual = myutils.splitIntoLines(text, true);
                assert.equal(actual, expected);
            });
        });
    })
})