import os from "os";
import path from "path";

import { assert } from "chai";

import * as myutils from "../src/helperUtils";

describe('myutils', function() {

    describe('#splitIntoLines', function() {
        let data = [
            { text: "", expected: [] },
            { text: "\n", expected: ["", "\n"] },
            { text: "abc", expected: ["abc", ""] },
            { text: "ab\nc", expected: ["ab", "\n", "c", ""] },
            { text: "ab\nc\r\n", expected: ["ab", "\n", "c", "\r\n"]}
        ];
        data.forEach(({ text, expected }, i) => {
            it(`should pass with separateTerminators and with input ${i}`, function() {
                const actual = myutils.splitIntoLines(text, true);
                assert.deepEqual(actual, expected);
            });
        });
        data = [
            { text: "", expected: [] },
            { text: "\n", expected: ["\n"] },
            { text: "abc", expected: ["abc"] },
            { text: "ab\nc", expected: ["ab\n", "c"] },
            { text: "ab\nc\r\n", expected: ["ab\n", "c\r\n"]}
        ];
        data.forEach(({ text, expected }, i) => {
            it(`should pass without separateTerminators and with input ${i}`, function() {
                const actual = myutils.splitIntoLines(text, false);
                assert.deepEqual(actual, expected);
            });
        });
    });

    describe('#determineIndent', function() {
        let data = [
            { text: " |", expected: " " },
            { text: "", expected: "" },
            { text: " ", expected: " " },
            { text: "\t", expected: "\t" },
            { text: "  \f\r\n", expected: "  \f\r\n" },
            { text: "\r", expected: "\r" },
            { text: "(d2", expected: "" },
            { text: "0", expected: "" },
            { text: "_", expected: "" }
        ];
        data.forEach(({ text, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = myutils.determineIndent(text);
                assert.deepEqual(actual, expected);
            });
        });
    });

    describe('#isBlank', function() {
        let data = [
            { text: " |", expected: false },
            { text: "", expected: true },
            { text: null, expected: true },
            { text: " ", expected: true },
            { text: "\t", expected: true },
            { text: "  \f\r\n", expected: true },
            { text: "\r", expected: true },
            { text: "(d2", expected: false },
            { text: "0", expected: false },
            { text: "_", expected: false }
        ];
        data.forEach(({ text, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = myutils.isBlank(text);
                assert.deepEqual(actual, expected);
            });
        });
    });

    describe('#modifyNameToBeAbsent', function() {
        let data = [
            { names: [], originalName: "nay", expected: "nay" },
            { names: [], originalName: "", expected: "" },
            { names: ["nay"], originalName: "nay", expected: "nay-1" },
            { names: ["so", "hi", "hi-1"], originalName: "hi", expected: "hi-2" },
            { names: [""], originalName: "", expected: "-1" },
            { names: ["sand", "sand-1", "sand-2"], originalName: "sand-1", expected: "sand-1-1" },
        ];
        data.forEach(({ names, originalName, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = myutils.modifyNameToBeAbsent(names, originalName);
                assert.deepEqual(actual, expected);
            });
        });
        it(`should pass after many retries`, function() {
            const names = ["n",
                "n-1", "n-2", "n-3", "n-4", "n-5",
                "n-6", "n-7", "n-8", "n-9"];
            const actual = myutils.modifyNameToBeAbsent(names, "n");
            assert.equal(actual.length, 10);
        });
    });

    describe('#modifyTextToBeAbsent', function() {
        let data = [
            { target: [], originalText: "nay", expected: "nay" },
            { target: [], originalText: "", expected: "" },
            { target: ["nay"], originalText: "nay", expected: "nay-1" },
            { target: ["sohi", "hi-1"], originalText: "hi", expected: "hi-2" },
            { target: [""], originalText: "", expected: "-1" },
            { target: ["sand", "sand-1", "sand-2"], originalText: "sand-1", expected: "sand-1-1" },
            { target: ["sandsand-1", "sand1-1sand-2"], originalText: "sand1", expected: "sand1-2" },
        ];
        data.forEach(({ target, originalText, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = myutils.modifyTextToBeAbsent(target, originalText);
                assert.deepEqual(actual, expected);
            });
        });
        it(`should pass after many retries`, function() {
            const target = ["n",
                "n-1n-2n-3n-4n-5",
                "n-6n-7n-8n-9"];
            const actual = myutils.modifyTextToBeAbsent(target, "n");
            assert.equal(actual.length, 10);
        });
    });

    describe('#generateValidFileName', function() {
        let data = [
            { p: "dk", expected: "dk" },
            { p: "-._", expected: "-_" },
            // omitted these since their results vary according to OS platform
            //{ p: "c:\\", expected: "c" },
            //{ p: "d:\\", expected: "d" },
            { p: "..", expected: "c" },
            { p: ":\\", expected: "c" },
            { p: ".", expected: "c" },
            { p: "", expected: "c" },
            { p: "f/ck", expected: "ck" },
            { p: "f/ck/", expected: "ck" },
            { p: "var\\f\\k.txt", expected: "ktxt" }
        ];
        data.forEach(({ p, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = myutils._generateValidFileName(p);
                assert.deepEqual(actual, expected);
            });
        });
    });

    describe('#_splitFilePath', function() {
        let data = [
            { fullPath: "/dk/d", baseDir:"/dk",
                expected: { baseDir:"/dk", relativePath: "d"} },
            { fullPath: "d:\\k\\d", baseDir:"d:\\k",
                expected: { baseDir:"d:\\k", relativePath: "d"} },
            { fullPath: "/dk/ef/fg/d", baseDir:"/dk/ef",
                expected: { baseDir:"/dk/ef", relativePath: "fg/d"} },
            { fullPath: "d:\\k\\ef\\fgh\\d", baseDir:"d:\\k\\ef",
                expected: { baseDir:"d:\\k\\ef", relativePath: "fgh\\d"} },
            { fullPath: "/u/i/o", baseDir:"/u/c/t",
                expected: { baseDir:"/u/i/", relativePath: "o"} },
            { fullPath: "c:\\users\\paa", baseDir:"c:\\users\\nii",
                expected: { baseDir:"c:\\users\\", relativePath: "paa"} },
            { fullPath: "c:\\users\\paa", baseDir: null,
                expected: { baseDir:"c:\\users\\", relativePath: "paa"} },
            { fullPath: "/home/users/akwasi/docs", baseDir: null,
                expected: { baseDir:"/home/users/akwasi/", relativePath: "docs"} },
        ];
        data.forEach(({ fullPath, baseDir, expected }, i) => {
            it(`should pass with input ${i}`, function() {
                const actual = myutils._splitFilePath(fullPath, baseDir);
                assert.deepEqual(actual, expected);
            });
        });
        it('should fail', function() {
            assert.throws(function() {
                myutils._splitFilePath("dlll", "tea");
            });
        });
    });

    describe('#normalizeSrcFileLoc', function() {
        it('should pass with baseDir absent', function() {
            const baseDir = "";
            const relativePath = "c";
            const cwd = process.cwd();
            const expected = {
                baseDir: cwd + "/",
                relativePath: "c"
            };
            const slashRegex = new RegExp("/|\\\\", "g");
            const expectedBaseDir1 = expected.baseDir.replace(slashRegex, "/");
            const expectedBaseDir2 = expected.baseDir.replace(slashRegex, "\\");
            const expectedRelativePath1 = expected.relativePath.replace(slashRegex, "/");
            const expectedRelativePath2 = expected.relativePath.replace(slashRegex, "\\");
            const actual = myutils.normalizeSrcFileLoc(baseDir, relativePath);
            assert.deepInclude([
                { baseDir: expectedBaseDir1, relativePath: expectedRelativePath1 },
                { baseDir: expectedBaseDir2, relativePath: expectedRelativePath2 }], actual);
        });
        it('should pass with baseDir present', function() {
            const cwd = process.cwd();
            const cwdParent = path.dirname(cwd);
            if (!cwdParent) {
                return;
            }
            const baseDir = cwd;
            const relativePath = "../75ef072a-51e9-4950-b0ad-c183c5146a2c/d/e/f";
            const expected = {
                baseDir: cwdParent + "/75ef072a-51e9-4950-b0ad-c183c5146a2c/d/e/",
                relativePath: "f"
            };
            const slashRegex = new RegExp("/|\\\\", "g");
            const expectedBaseDir1 = expected.baseDir.replace(slashRegex, "/");
            const expectedBaseDir2 = expected.baseDir.replace(slashRegex, "\\");
            const expectedRelativePath1 = expected.relativePath.replace(slashRegex, "/");
            const expectedRelativePath2 = expected.relativePath.replace(slashRegex, "\\");
            const actual = myutils.normalizeSrcFileLoc(baseDir, relativePath);
            assert.deepInclude([
                { baseDir: expectedBaseDir1, relativePath: expectedRelativePath1 },
                { baseDir: expectedBaseDir2, relativePath: expectedRelativePath2 }], actual);
        });
    });

    describe('#printNormalDiff', function() {
        it('should pass with input 0', function() {
            const x = [""];
            const y = [""];
            const expected = "";
            const actual = myutils.printNormalDiff(x, y);
            assert.deepEqual(actual, expected);
        });
        it('should pass with input 1', function() {
            const x = ["my"];
            const y = ["my"];
            const expected = "";
            const actual = myutils.printNormalDiff(x, y);
            assert.deepEqual(actual, expected);
        });
        it('should pass with input 2', function() {
            const x = ["my\n", "o\n", "mine!\r\n"];
            const y = ["my\n", "o\n", "mine!\r\n"];
            const expected = "";
            const actual = myutils.printNormalDiff(x, y);
            assert.deepEqual(actual, expected);
        });
        it('should pass with input 3', function() {
            const x = ["This part of the\r\n", 
                "document has stayed the\r\n",
                "same from version to\r\n",
                "version.  It shouldn't\r\n",
                "be shown if it doesn't\r\n",
                "change.  Otherwise, that\r\n",
                "would not be helping to\r\n",
                "compress the size of the\r\n",
                "changes.\r\n",
                "\r\n",
                "This paragraph contains\r\n",
                "text that is outdated.\r\n",
                "It will be deleted in the\r\n",
                "near future.\r\n",
                "\r\n",
                "It is important to spell\r\n",
                "check this dokument. On\r\n",
                "the other hand, a\r\n",
                "misspelled word isn't\r\n",
                "the end of the world.\r\n",
                "Nothing in the rest of\r\n",
                "this paragraph needs to\r\n",
                "be changed. Things can\r\n",
                "be added after it."];
            const y = ["This is an important\r\n",
                "notice! It should\r\n",
                "therefore be located at\r\n",
                "the beginning of this\r\n",
                "document!\r\n",
                "\r\n",
                "This part of the\r\n", 
                "document has stayed the\r\n",
                "same from version to\r\n",
                "version.  It shouldn't\r\n",
                "be shown if it doesn't\r\n",
                "change.  Otherwise, that\r\n",
                "would not be helping to\r\n",
                "compress the size of the\r\n",
                "changes.\r\n",
                "\r\n",
                "It is important to spell\r\n",
                "check this document. On\r\n",
                "the other hand, a\r\n",
                "misspelled word isn't\r\n",
                "the end of the world.\r\n",
                "Nothing in the rest of\r\n",
                "this paragraph needs to\r\n",
                "be changed. Things can\r\n",
                "be added after it.\r\n",
                "\r\n",
                "This paragraph contains\r\n",
                "important new additions\r\n",
                "to this document."];
            const expected = ["0a1,6",
                "> This is an important",
                "> notice! It should",
                "> therefore be located at",
                "> the beginning of this",
                "> document!",
                "> ",
                "11,15d16",
                "< This paragraph contains",
                "< text that is outdated.",
                "< It will be deleted in the",
                "< near future.",
                "< ",
                "17c18",
                "< check this dokument. On",
                "---",
                "> check this document. On",
                "24c25,29",
                "< be added after it.",
                "\\ No newline at end of file",
                "---",
                "> be added after it.",
                "> ",
                "> This paragraph contains",
                "> important new additions",
                "> to this document.",
                "\\ No newline at end of file",
                ""].join(os.EOL);
            const actual = myutils.printNormalDiff(x, y);
            assert.deepEqual(actual, expected);
        });
        it('should pass with input 4', function() {
            const x = ["The Way that can be told of is not the eternal Way;\n",
                "The name that can be named is not the eternal name.\n",
                "The Nameless is the origin of Heaven and Earth;\n",
                "The Named is the mother of all things.\n",
                "Therefore let there always be non-being,\n",
                "  so we may see their subtlety,\n",
                "And let there always be being,\n",
                "  so we may see their outcome.\n",
                "The two are the same,\n",
                "But after they are produced,\n",
                "  they have different names.\n"];
            const y = ["The Nameless is the origin of Heaven and Earth;\n",
                "The named is the mother of all things.\n",
                "\n",
                "Therefore let there always be non-being,\n",
                "  so we may see their subtlety,\n",
                "And let there always be being,\n",
                "  so we may see their outcome.\n",
                "The two are the same,\n",
                "But after they are produced,\n",
                "  they have different names.\n",
                "They both may be called deep and profound.\n",
                "Deeper and more profound,\n",
                "The door of all subtleties!\n"];
            const expected = ["1,2d0",
                "< The Way that can be told of is not the eternal Way;",
                "< The name that can be named is not the eternal name.",
                "4c2,3",
                "< The Named is the mother of all things.",
                "---",
                "> The named is the mother of all things.",
                "> ",
                "11a11,13",
                "> They both may be called deep and profound.",
                "> Deeper and more profound,",
                "> The door of all subtleties!",
                ""].join(os.EOL);
            const actual = myutils.printNormalDiff(x, y);
            assert.deepEqual(actual, expected);
        });
    });
});