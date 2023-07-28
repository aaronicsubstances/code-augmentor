import { assert } from "chai";

import CodeChangeDetective from "../src/CodeChangeDetective";
import { SourceFileDescriptor } from "../src/types";

describe("CodeChangeDetective", function() {
    it("should pass", async function() {
        const instance = new CodeChangeDetective();
        /*const src = (async function*() {
            const s : SourceFileDescriptor = {
                baseDir: "d",
                relativePath: "t.txt",
                content: "did",
                encoding: null,
                binaryContent: null
            };
            yield s;
        })();*/
        const expected = false;
        const actual = await instance.execute();
        assert.equal(actual, expected);
    });
});
