
import os from "os";
import path from "path";

import { assert } from "chai";

import CodeChangeDetective from "../src/CodeChangeDetective";
import { SourceFileDescriptor } from "../src/types";

function createTempDir(name: string) {
    return path.join(os.tmpdir(), "code-augmentor-nodejs", name);
}

describe("CodeChangeDetective", function() {
    it("should pass with no props set", async function() {
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

    it("should pass with defaults and no items supplied", async function() {
        const instance = new CodeChangeDetective();
        instance.destDir = createTempDir("room1");
        const cleanUp = await instance.defaultSetup();
        const expected = false;
        let actual = false;
        try {
            actual = await instance.execute();
        }
        finally {
            await cleanUp();
        }
        assert.equal(actual, expected);
    });
});
