import os from "os";
import path from "path";

import { assert } from "chai";

import {
    CodeChangeDetective, DefaultCodeChangeDetectiveConfig
} from "../src/CodeChangeDetective";
import * as myutils from "../src/myutils";
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

function getTempPath(...names: string[]) {
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
        assert.isEmpty(config.changeSummaryLogs);
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

        // act
        const actual = await instance.execute();

        // assert
        assert.isFalse(actual);
        assert.isTrue(config.releaseCalled);
        assert.deepEqual(config.outputSummaryLogs, expectedOutputSummaryLogs);
        assert.isEmpty(config.changeSummaryLogs);
        assert.isEmpty(config.changeDiffLogs);
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
        assert.isEmpty(config.changeDiffLogs);
        assert.deepEqual(toObject(config.fileContent), expectedFileContent);
    });

    it("should pass with code change enabled (3)", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        const configFactory = new TestCodeChangeDetectiveConfigFactory();
        instance.configFactory = configFactory;
        const config = new TestCodeChangeDetectiveConfig();
        configFactory.config = config;
        config.fileContent.set("room1", generateHex("drink"));
        config.fileContent.set("sea\\2", generateHex("hello"));
        config.fileContent.set("rty", generateHex("world\n"));
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
                content: "worlds\n",
                isBinary: false
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
        const expectedFileContent = Object.assign({
            '1-sea\\2': generateHex('ello'),
            '2-rty': generateHex('worlds\n')
        }, toObject(config.fileContent));
        const expectedOutputSummaryLogs = [
            `room1${os.EOL}${os.EOL}`,
            `sea\\2${os.EOL}1-sea\\2${os.EOL}`,
            `rty${os.EOL}2-rty${os.EOL}`,
            `drinks/local${os.EOL}${os.EOL}`];
        const expectedChangeSummaryLogs = [
            `sea\\2${os.EOL}1-sea\\2${os.EOL}`,
            `rty${os.EOL}2-rty${os.EOL}`];
        const expectedChangeDiffLogs = [
            `${os.EOL}--- sea\\2${os.EOL}+++ 1-sea\\2${os.EOL}` +
                `${os.EOL}Binary files differ${os.EOL}`,
            `${os.EOL}--- rty${os.EOL}+++ 2-rty${os.EOL}` +
                `1c1${os.EOL}< world${os.EOL}---${os.EOL}> worlds${os.EOL}`
        ];

        // act
        const actual = await instance.execute();

        // assert
        assert.isTrue(actual);
        assert.isTrue(config.releaseCalled);
        assert.deepEqual(config.outputSummaryLogs, expectedOutputSummaryLogs);
        assert.deepEqual(config.changeSummaryLogs, expectedChangeSummaryLogs);
        assert.deepEqual(config.changeDiffLogs, expectedChangeDiffLogs);
        assert.deepEqual(toObject(config.fileContent), expectedFileContent);
    });

    it("should pass with error report enabled", async function() {
        // arrange
        const instance = new CodeChangeDetective();
        const configFactory = new TestCodeChangeDetectiveConfigFactory();
        instance.configFactory = configFactory;
        const config = new TestCodeChangeDetectiveConfig();
        configFactory.config = config;
        config.fileContent.set("room\\A", generateHex("water"));
        const src: SourceFileDescriptor[] = [
            {
                baseDir: "room",
                relativePath: "1",
                content: 'drink',
                isBinary: false
            },
            {
                baseDir: "room\\",
                relativePath: "A",
                content: 'water',
                isBinary: false
            },
            {
                baseDir: "office",
                relativePath: "6",
                content: 'drinks',
                isBinary: false
            }
        ];
        instance.srcFileDescriptors = src;
        instance.codeChangeDetectionEnabled = true;
        const reportedErrors = new Array<any>();
        const reportedErrorMessages = new Array<string>();
        instance.reportError = async (e, m) => {
            reportedErrors.push(e);
            reportedErrorMessages.push(m);
        };
        const expectedOutputSummaryLogs = [
            `room\\A${os.EOL}${os.EOL}`
        ];
        const expectedFileContent = toObject(config.fileContent);

        // act
        const actual = await instance.execute();

        // assert
        assert.isFalse(actual);
        assert.isTrue(config.releaseCalled);
        assert.deepEqual(config.outputSummaryLogs, expectedOutputSummaryLogs);
        assert.isEmpty(config.changeSummaryLogs);
        assert.isEmpty(config.changeDiffLogs);
        assert.deepEqual(toObject(config.fileContent), expectedFileContent);

        // assert reported errors
        assert.equal(reportedErrorMessages.length, 2);
        assert.include(reportedErrorMessages[0], "0:");
        assert.include(reportedErrorMessages[1], "2:");
        assert.equal(reportedErrors.length, 2);
        assert.include(reportedErrors[0].message, "room1");
        assert.include(reportedErrors[1].message, "office6");
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
        let actualError: any;
        try {
            await instance.execute();
        }
        catch (e) {
            actualError = e;
        }
        assert.include(actualError.message, "room1");
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
        let actualError: any;
        try {
            await instance.execute();
        }
        catch (e) {
            actualError = e;
        }
        assert.isOk(actualError);
        assert.include(actualError.message, "configFactory");
        assert.include(actualError.message, "not set");
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
        let actualError: any;
        try {
            await instance.execute();
        }
        catch (e) {
            actualError = e;
        }
        assert.isOk(actualError);
        assert.include(actualError.message, "null config");
    });
});

describe("DefaultCodeChangeDetectiveConfig", function() {
    describe("#release", function() {
        it(`should pass if no appending was done`, async function() {
            // arrange
            const instance = new DefaultCodeChangeDetectiveConfig();
            
            // act
            await instance.release();

            // assert error in appending after releasing.
            let actualError: any;
            try {
                await instance.appendOutputSummary("data")
            }
            catch (e) {
                actualError = e;
            }
            assert.isOk(actualError);

            actualError = null;
            try {
                await instance.appendChangeSummary("portion")
            }
            catch (e) {
                actualError = e;
            }
            assert.isOk(actualError);

            actualError = null;
            try {
                await instance.appendChangeDiff("ample")
            }
            catch (e) {
                actualError = e;
            }
            assert.isOk(actualError);
        })
        it (`should pass after appending`, async function () {
            // arrange
            const instance = new DefaultCodeChangeDetectiveConfig();
            instance.destDir = getTempPath("DefaultCodeChangeDetectiveConfig",
                "testReleaseAfterAppending");
            await myutils.cleanDir(instance.destDir);
            const expectedOutputSummary = "\u0186d\u0254";
            const expectedChangeSummary = "Y\u0190";
            const expectedChangeDiff = "ade\u025b a ehia";

            // act
            await instance.appendOutputSummary(expectedOutputSummary)
            await instance.appendChangeSummary(expectedChangeSummary)
            await instance.appendChangeDiff(expectedChangeDiff)
            await instance.release();

            // assert
            const actualOutputSummary = await instance.getFileContent({
                baseDir: instance.destDir,
                relativePath: "output-summary.txt"
            }, false, "utf8");
            assert.equal(actualOutputSummary, expectedOutputSummary);
            const actualChangeSummary = await instance.getFileContent({
                baseDir: instance.destDir,
                relativePath: "change-summary.txt"
            }, false, "utf8");
            assert.equal(actualChangeSummary, expectedChangeSummary);
            const actualChangeDiff = await instance.getFileContent({
                baseDir: instance.destDir,
                relativePath: "change-diff.txt"
            }, false, "utf8");
            assert.equal(actualChangeDiff, expectedChangeDiff);

            // assert error in appending after releasing.
            let actualError: any;
            try {
                await instance.appendOutputSummary("data")
            }
            catch (e) {
                actualError = e;
            }
            assert.isOk(actualError);

            actualError = null;
            try {
                await instance.appendChangeSummary("portion")
            }
            catch (e) {
                actualError = e;
            }
            assert.isOk(actualError);

            actualError = null;
            try {
                await instance.appendChangeDiff("ample")
            }
            catch (e) {
                actualError = e;
            }
            assert.isOk(actualError);
        })
    });
    describe("#normalizeSrcFileLoc", function() {
        it('should pass with baseDir set', function() {
            const instance = new DefaultCodeChangeDetectiveConfig();
            const tempPath = getTempPath("normalizeSrcFileLoc1");
            const srcFileDescriptor = {
                baseDir: tempPath,
                relativePath: "specific1"
            } as SourceFileDescriptor;
            const expected: SourceFileLocation = {
                baseDir: tempPath,
                relativePath: "specific1"
            }
            const actual = instance.normalizeSrcFileLoc(srcFileDescriptor);
            assert.deepEqual(actual, expected);
        })
        it('should pass without baseDir set', function() {
            const instance = new DefaultCodeChangeDetectiveConfig();
            const tempPath = getTempPath("normalizeSrcFileLoc2");
            const srcFileDescriptor = {
                relativePath: tempPath + path.sep + "actual2"
            } as SourceFileDescriptor;
            const expected: SourceFileLocation = {
                baseDir: tempPath + path.sep,
                relativePath: "actual2"
            }
            const actual = instance.normalizeSrcFileLoc(srcFileDescriptor);
            assert.deepEqual(actual, expected);
        })
    })

    describe("#stringifySrcFileLoc", function() {
        it('should pass', function() {
            const instance = new DefaultCodeChangeDetectiveConfig();
            const tempPath = getTempPath("stringifySrcFileLoc");
            const loc: SourceFileLocation = {
                baseDir: tempPath,
                relativePath: "specific"
            }
            const expected = loc.baseDir + path.sep + loc.relativePath; 
            const actual = instance.stringifySrcFileLoc(loc);
            assert.equal(actual, expected);
        })
    })

    describe("#stringifyDestFileLoc", function() {
        it('should pass', function() {
            const instance = new DefaultCodeChangeDetectiveConfig();
            instance.destDir = getTempPath("stringifyDestFileLoc");
            const loc: SourceFileLocation = {
                baseDir: "broom",
                relativePath: "specific"
            }
            const expected = instance.destDir + path.sep +
                loc.baseDir + path.sep + loc.relativePath; 
            const actual = instance.stringifyDestFileLoc(loc);
            assert.equal(actual, expected);
        })
    })

    describe("#areFileContentsEqual", function() {
        let data = [
            { arg1: "", arg2: "", isBinary: false, expected: true },
            { arg1: "apple", arg2: "apple", isBinary: false, expected: true },
            { arg1: "apple", arg2: "Apple", isBinary: false, expected: false },
            { arg1: "apple", arg2: "banana", isBinary: false, expected: false },
            { arg1: Buffer.from("apple"), arg2: Buffer.from("apple"),
                isBinary: true, expected: true },
            { arg1: Buffer.from("apple"), arg2: Buffer.from("banana"),
                isBinary: true, expected: false },
        ];
        data.forEach(({arg1, arg2, isBinary, expected}, i) => {
            it(`should pass with input ${i}`, function() {
                const instance = new DefaultCodeChangeDetectiveConfig();
                const actual = instance.areFileContentsEqual(arg1, arg2, isBinary);
                assert.equal(actual, expected);
            })
        })
    })

    describe("#generateDestFileLoc", function() {
        let data = [
            {
                srcFileLoc: { baseDir: 'c:\\', relativePath: 'barbecue\\hot'},
                expected: { baseDir: 'c', relativePath: 'barbecue\\hot'},
            },
            {
                srcFileLoc: { baseDir: 'd:\\', relativePath: 'barbecue\\hot'},
                expected: { baseDir: 'c-1', relativePath: 'barbecue\\hot'},
            },
            {
                srcFileLoc: { baseDir: 'd:\\barbecue', relativePath: 'hot'},
                expected: { baseDir: 'barbecue', relativePath: 'hot'},
            },
            {
                srcFileLoc: { baseDir: '/', relativePath: ''},
                expected: { baseDir: 'c-2', relativePath: ''},
            },
            {
                srcFileLoc: { baseDir: '/barbecue', relativePath: 'really/hot'},
                expected: { baseDir: 'barbecue-1', relativePath: 'really/hot'},
            },
            {
                srcFileLoc: { baseDir: 'dog', relativePath: 'bull/run'},
                expected: { baseDir: 'dog', relativePath: 'bull/run'},
            },
            {
                srcFileLoc: { baseDir: 'dog', relativePath: 'bull'},
                expected: { baseDir: 'dog', relativePath: 'bull'},
            },
            {
                srcFileLoc: { baseDir: 'my/dog', relativePath: 'bull'},
                expected: { baseDir: 'dog-1', relativePath: 'bull'},
            },
            {
                srcFileLoc: { baseDir: 'my/dog', relativePath: 'cat'},
                expected: { baseDir: 'dog-1', relativePath: 'cat'},
            },
            {
                srcFileLoc: { baseDir: 'his/dog', relativePath: 'trim'},
                expected: { baseDir: 'dog-2', relativePath: 'trim'},
            },
            {
                srcFileLoc: { baseDir: 'his/dog', relativePath: 'train'},
                expected: { baseDir: 'dog-2', relativePath: 'train'},
            },
            {
                srcFileLoc: { baseDir: 'hisdog', relativePath: 'trim'},
                expected: { baseDir: 'hisdog', relativePath: 'trim'},
            }
        ]
        const instance = new DefaultCodeChangeDetectiveConfig();
        data.forEach(({srcFileLoc, expected}, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = instance.generateDestFileLoc(srcFileLoc);
                assert.deepEqual(actual, expected);
            })
        })
    })
});