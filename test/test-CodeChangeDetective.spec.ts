import os from "os";
import path from "path";

import { assert } from "chai";

import {
    CodeChangeDetective
} from "../src/CodeChangeDetective";
import {
    CodeChangeDetectiveConfig,
    CodeChangeDetectiveConfigFactory,
    SourceFileDescriptor,
    SourceFileLocation
} from "../src/types";

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
    changeSummaryLogs = new Array<string>();
    changeDiffLogs = new Array<string>();
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

    async appendChangeSummary(data: string) {
        this.changeSummaryLogs.push(data);
    }

    async appendChangeDiff(data: string) {
        this.changeDiffLogs.push(data);
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
    it("should pass with no items available", async function() {
        const instance = new CodeChangeDetective();
        const actual = await instance.execute();
        assert.isFalse(actual);
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
        config.appendChangeSummary = () => {
            throw new Error("appendChangeSummary should not be called");
        };
        config.appendChangeDiff = () => {
            throw new Error("appendChangeDiff should not be called");
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
        assert.isFalse(actual);
        assert.isTrue(config.releaseCalled);
        assert.deepEqual(config.outputSummaryLogs, expectedOutputSummaryLogs);
        assert.isEmpty(config.changeDiffLogs);
        assert.deepEqual(toObject(config.fileContent), expectedFileContent);
    });

    it("should pass with code change enabled (1)", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        const configFactory = new TestCodeChangeDetectiveConfigFactory();
        instance.configFactory = configFactory;
        const config = new TestCodeChangeDetectiveConfig();
        configFactory.config = config;
        config.appendChangeDiff = () => {
            throw new Error("appendChangeDiff should not be called");
        };
        config.fileContent.set("room1", generateHex("drink"));
        config.fileContent.set("sea\\2", generateHex("hello"));
        config.fileContent.set("rty", generateHex("world"));
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink',
                isBinary: false
            },
            {
                baseDir: "sea\\",
                relativePath: "2",
                binaryContent: Buffer.from("hello"),
                isBinary: true
            },
            {
                relativePath: "rty",
                binaryContent: Buffer.from("world"),
                isBinary: true
            }
        ];
        instance.srcFileDescriptors = src;
        instance.reportError = null as any;
        const expectedFileContent = toObject(config.fileContent);
        const expectedOutputSummaryLogs = [
            `room1${os.EOL}${os.EOL}`,
            `sea\\2${os.EOL}${os.EOL}`,
            `rty${os.EOL}${os.EOL}`];
        const expectedChangeSummaryLogs = [];

        // act
        const actual = await instance.execute();

        // assert
        assert.isFalse(actual);
        assert.isTrue(config.releaseCalled);
        assert.deepEqual(config.outputSummaryLogs, expectedOutputSummaryLogs);
        assert.deepEqual(config.changeSummaryLogs, expectedChangeSummaryLogs);
        assert.deepEqual(toObject(config.fileContent), expectedFileContent);
    });

    it("should pass with code change enabled (2)", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        const configFactory = new TestCodeChangeDetectiveConfigFactory();
        instance.configFactory = configFactory;
        const config = new TestCodeChangeDetectiveConfig();
        configFactory.config = config;
        config.appendChangeDiff = null as any;
        config.fileContent.set("room1", generateHex("drink"));
        config.fileContent.set("sea\\2", generateHex("hello"));
        config.fileContent.set("rty", generateHex("world"));
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
                binaryContent: Buffer.from("ello"),
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
        instance.codeChangeDetectionEnabled = true;
        instance.reportError = undefined;
        const expectedFileContent = Object.assign({
            '1-sea\\2': generateHex('ello'),
            '2-rty': generateHex('worlds')
        }, toObject(config.fileContent));
        const expectedOutputSummaryLogs = [
            `room1${os.EOL}${os.EOL}`,
            `sea\\2${os.EOL}1-sea\\2${os.EOL}`,
            `rty${os.EOL}2-rty${os.EOL}`,
            `drinks/local${os.EOL}${os.EOL}`];
        const expectedChangeSummaryLogs = [
            `sea\\2${os.EOL}1-sea\\2${os.EOL}`,
            `rty${os.EOL}2-rty${os.EOL}`];

        // act
        const actual = await instance.execute();

        // assert
        assert.isTrue(actual);
        assert.isTrue(config.releaseCalled);
        assert.deepEqual(config.outputSummaryLogs, expectedOutputSummaryLogs);
        assert.deepEqual(config.changeSummaryLogs, expectedChangeSummaryLogs);
        assert.deepEqual(toObject(config.fileContent), expectedFileContent);
    });

    it("should pass with error report enabled", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        const configFactory = new TestCodeChangeDetectiveConfigFactory();
        instance.configFactory = configFactory;
        const config = new TestCodeChangeDetectiveConfig();
        configFactory.config = config;
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink',
                isBinary: false
            }
        ];
        instance.srcFileDescriptors = src;
        let reportedError, reportedErrorMessage;
        instance.reportError = async (e, m) => {
            reportedError = e;
            reportedErrorMessage = m;
        };

        // act
        const actual = await instance.execute();

        // assert
        assert.isFalse(actual);
        assert.isTrue(config.releaseCalled);
        assert.isEmpty(config.outputSummaryLogs);
        assert.isEmpty(config.changeSummaryLogs);
        assert.isEmpty(config.changeDiffLogs);
        assert.include(reportedErrorMessage, "0:");
        assert.include(reportedError.message, "room1");
    });

    it("should fail with error report disabled", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        const configFactory = new TestCodeChangeDetectiveConfigFactory();
        instance.configFactory = configFactory;
        const config = new TestCodeChangeDetectiveConfig();
        configFactory.config = config;
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink',
                isBinary: false
            }
        ];
        instance.srcFileDescriptors = src;

        // act and assert
        try {
            await instance.execute();
            assert.fail("expected an error about not being able to get file room1")
        }
        catch (e) {
            assert.include(e.message, "room1");
        }
    });

    it("should fail with config factory not set", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        instance.configFactory = null as any;
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink',
                isBinary: false
            }
        ];
        instance.srcFileDescriptors = src;

        // act and assert
        try {
            await instance.execute();
            assert.fail("expected an error about not being able to get file room1")
        }
        catch (e) {
            assert.include(e.message, "configFactory");
            assert.include(e.message, "not set");
        }
    });

    it("should fail with finding null config", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        instance.configFactory = new TestCodeChangeDetectiveConfigFactory()
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink',
                isBinary: false
            }
        ];
        instance.srcFileDescriptors = src;

        // act and assert
        try {
            await instance.execute();
            assert.fail("expected an error about not being able to get file room1")
        }
        catch (e) {
            assert.include(e.message, "null config");
        }
    });
});
