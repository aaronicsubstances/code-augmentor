import os from "os";
import path from "path";

import { assert } from "chai";

import { CodeChangeDetective, DefaultCodeChangeDetectiveConfigFactory } from "../src/CodeChangeDetective";
import { CodeChangeDetectiveConfig, CodeChangeDetectiveConfigFactory, SourceFileDescriptor, SourceFileLocation } from "../src/types";

function generateHex(s: string, encoding?: BufferEncoding): string {
    return Buffer.from(s, encoding || "utf8").toString('hex');
}

function decodeHex(s: string): string {
    return Buffer.from(s, "hex").toString('utf8');
}

function createTempPath(...names: string[]) {
    return path.resolve(os.tmpdir(), "code-augmentor-nodejs", ...names);
}

function toObject(map: any): any {
    const obj = {};
    for (const item of map) {
        obj[item[0]] = item[1];
    }
    return obj;
}

class TestCodeChangeDetectiveConfig implements CodeChangeDetectiveConfig {
    releaseCalled = false;
    outputSummaryLogs = new Array<string>();
    changeDetailsLogs = new Array<string>();
    fileContent = new Map<string, string>();
    _seed = 0;

    async release(): Promise<void> {
        this.releaseCalled = true;
    }

    async getFileContent(loc: SourceFileLocation, isBinary: boolean,
            encoding?: BufferEncoding): Promise<string | Buffer> {
        const p = this.stringifySrcFileLoc(loc);
        const c = this.fileContent.get(p);
        if (!c) {
            throw new Error("could not find file " + p);
        }
        const buf = Buffer.from(c, 'hex');
        if (isBinary) {
            return buf;
        }
        return buf.toString(encoding);
    }

    async saveFileContent(loc: SourceFileLocation, data: string | Buffer,
            isBinary: boolean, encoding?: BufferEncoding): Promise<void> {
        const p = this.stringifyDestFileLoc(loc);
        let b;
        if (isBinary) {
            b = data as Buffer;
        }
        else {
            b = Buffer.from(data as string, encoding);
        }
        b = b.toString('hex');
        this.fileContent.set(p, b);
    }

    async appendOutputSummary(data: string) {
        this.outputSummaryLogs.push(data);
    }

    async appendChangeDetails(data: string) {
        this.changeDetailsLogs.push(data);
    }

    normalizeSrcFileLoc(srcFileDescriptor: SourceFileDescriptor) {
        const result: SourceFileLocation = {
            baseDir: srcFileDescriptor.baseDir || '',
            relativePath: srcFileDescriptor.relativePath
        };
        return result;
    }

    stringifySrcFileLoc(loc: SourceFileLocation) {
        return `${loc.baseDir || ''}${loc.relativePath}`;
    }

    stringifyDestFileLoc(loc: SourceFileLocation) {
        return `${loc.baseDir || ''}${loc.relativePath}`;
    }

    areFileContentsEqual(arg1: string | Buffer, arg2: string | Buffer, isBinary: boolean) {
        if (isBinary) {
            return Buffer.compare(arg1 as Buffer, arg2 as Buffer) === 0;
        }
        return arg1 === arg2;
    }

    generateDestFileLoc(srcFileLoc: SourceFileLocation) {
        this._seed++;
        const result: SourceFileLocation = {
            baseDir: this._seed + '-' + (srcFileLoc.baseDir || ''),
            relativePath: srcFileLoc.relativePath
        };
        return result;
    }
}

class TestCodeChangeDetectiveConfigFactory implements CodeChangeDetectiveConfigFactory {
    config?: CodeChangeDetectiveConfig;
    async create(): Promise<CodeChangeDetectiveConfig> {
        return this.config as any;
    }
};

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

    it("should pass with code change disabled (1)", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        const configFactory = new TestCodeChangeDetectiveConfigFactory();
        instance.configFactory = configFactory;
        const config = new TestCodeChangeDetectiveConfig();
        configFactory.config = config;
        config.getFileContent = () => {
            throw new Error("getFileContent should not be called");
        };
        config.areFileContentsEqual = () => {
            throw new Error("areFileContentsEqual should not be called");
        };
        config.appendChangeDetails = () => {
            throw new Error("appendChangeDetails should not be called");
        };
        config.fileContent.set("room1", generateHex("drink"));
        config.fileContent.set("2", generateHex("hello"));
        config.fileContent.set("3", generateHex("world"));
        const colaTextInHex = generateHex("cola", "utf16le");
        config.fileContent.set("drinks/local", colaTextInHex);
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink',
                isBinary: false
            },
            null as any,
            {
                baseDir: "sea\\",
                relativePath: "2",
                binaryContent: Buffer.from("hello"),
                isBinary: true
            },
            {
                relativePath: "rty",
                binaryContent: Buffer.from("worlds"),
                isBinary: true
            },
            {
                baseDir: "drinks/",
                relativePath: "local",
                content: 'cola',
                encoding: "utf16le",
                isBinary: false
            },
        ];
        instance.srcFileDescriptors = src;
        instance.codeChangeDetectionEnabled = false;
        instance.defaultEncoding = "ascii"
        instance.reportError = undefined;
        const expected = false;
        const expectedFileContent = Object.assign({
            '1-room1': generateHex('drink'),
            '2-sea\\2': generateHex('hello'),
            '3-rty': generateHex('worlds'),
            '4-drinks/local': colaTextInHex,
        }, toObject(config.fileContent));
        const expectedOutputSummaryLogs = [
            `room1${os.EOL}1-room1${os.EOL}`,
            `sea\\2${os.EOL}2-sea\\2${os.EOL}`,
            `rty${os.EOL}3-rty${os.EOL}`,
            `drinks/local${os.EOL}4-drinks/local${os.EOL}`];

        // act
        const actual = await instance.execute();

        // assert
        assert.equal(actual, expected);
        assert.isTrue(config.releaseCalled);
        assert.deepEqual(config.outputSummaryLogs, expectedOutputSummaryLogs);
        assert.isEmpty(config.changeDetailsLogs);
        assert.deepEqual(toObject(config.fileContent), expectedFileContent);
    });
});

