import os from "os";
import path from "path";

import { assert } from "chai";

import { CodeChangeDetective, DefaultCodeChangeDetectiveConfigFactory } from "../src/CodeChangeDetective";
import { SourceFileDescriptor } from "../src/types";

function createTempPath(...name: string[]) {
    return path.resolve(os.tmpdir(), "code-augmentor-nodejs", ...name);
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

    it("should pass with no items available", async function() {
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
        const configFactory = new DefaultCodeChangeDetectiveConfigFactory();
        configFactory.destDir = "dummy";
        instance.configFactory = configFactory;
        instance.srcFileDescriptors = [];
        const actual = await instance.execute();
        assert.equal(actual, expected);
    });

    /*it("should pass with nulls and code change disabled", async function() {
        const instance = new CodeChangeDetective();
        instance.codeChangeDetectionEnabled = false;
        instance.getFileContent = null as any
        instance.saveFileContent = null as any;
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink'
            },
            {
                relativePath: "2",
                binaryContent: Buffer.from("hello")
            },
            {
                relativePath: "3",
                binaryContent: Buffer.from("world")
            }
        ];
        instance.srcFileDescriptors = src;
        const expected = false;
        const actual = await instance.execute();
        assert.equal(actual, expected);
    });*/
});
