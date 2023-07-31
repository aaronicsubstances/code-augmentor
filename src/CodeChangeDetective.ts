import fs from "fs/promises";
import os from "os"
import path from "path";

import * as myutils from "./myutils";
import {
    SourceFileDescriptor, SourceFileLocation
} from "./types";

export default class CodeChangeDetective {
    private _destSubDirNameMap = new Map<string, string>();
    destDir = '';
    codeChangeDetectionEnabled = true;
    srcFileDescriptors?: AsyncIterable<SourceFileDescriptor>
        | Iterable<SourceFileDescriptor>;
    defaultEncoding?: BufferEncoding;
    appendOutputSummary?: (data: string) => Promise<void>;
    appendChangeSummary?: (data: string) => Promise<void>;
    appendChangeDetails?: (data: string) => Promise<void>;
    reportError?: (e: any, m: string) => Promise<void>;

    async getFileContent(loc: SourceFileLocation, isBinary: boolean, encoding?: BufferEncoding) {
        const p = path.join(loc.baseDir, loc.relativePath);
        if (isBinary) {
            return await fs.readFile(p);
        }
        else {
            return await fs.readFile(p, encoding);
        }
    }

    async saveFileContent(loc: SourceFileLocation, data: string | Buffer,
            isBinary: boolean, encoding?: BufferEncoding) {
        const p = path.join(this.destDir || '', loc.baseDir, loc.relativePath);
        await fs.mkdir(path.dirname(p), { recursive: true });
        if (isBinary) {
            await fs.writeFile(p, data);
        }
        else {
            await fs.writeFile(p, data, encoding);
        }
    }

    async defaultSetup(cleanDestDir: boolean = true) {
        const destDir = this.destDir;
        if (!destDir) {
            throw new Error("destDir property is null or invalid directory name");
        }
        if (cleanDestDir) {
            await fs.rm(destDir, { recursive: true, force: true });
            await fs.mkdir(destDir, { recursive: true });
        }

        // reset
        this._destSubDirNameMap = new Map<string, string>();

        const appendFileNames = ["OUTPUT-SUMMARY.txt", "CHANGE-SUMMARY.txt",
            "CHANGE-DETAILS.txt"];
        const appendFuncs = new Array<any>();
        const disposables = new Array<any>();
        for (const f of appendFileNames) {
            const disposable: any = {};
            disposables.push(disposable);
            const appendFunc = async (data: string) => {
                if (!disposable.w) {
                    const destDir = this.destDir;
                    if (!destDir) {
                        throw new Error("destDir property is null or invalid directory name");
                    }
                    disposable.w = await fs.open(path.join(destDir, f), "w");
                }
                await disposable.w.write(data);
            };
            appendFuncs.push(appendFunc);
        }
        this.appendOutputSummary = appendFuncs[0];
        this.appendChangeSummary = appendFuncs[1];
        this.appendChangeDetails = appendFuncs[2];
        return async () => {
            this.appendOutputSummary = undefined;
            this.appendChangeSummary = undefined;
            this.appendChangeDetails = undefined;
            for (const disposable of disposables) {
                if (disposable.w) {
                    await disposable.w.close();
                }
            }
        };
    }

    normalizeSrcFileLoc(baseDir: string | null | undefined, relativePath: string) {
        return myutils.normalizeSrcFileLoc(baseDir || null, relativePath);
    }

    stringifySrcFileLoc(loc: SourceFileLocation) {
        return path.resolve(loc.baseDir, loc.relativePath);
    }

    stringifyDestFileLoc(loc: SourceFileLocation) {
        return path.resolve(this.destDir || '', loc.baseDir, loc.relativePath);
    }

    areFileContentsEqual(arg1: any, arg2: any, isBinary: boolean) {
        if (!arg1 && !arg2) {
            return true;
        }
        if ((arg1 && !arg2) || (!arg1 && arg2)) {
            return false;
        }
        if (isBinary) {
            return Buffer.compare(arg1, arg2) === 0;
        }
        else {
            return arg1 === arg2;
        }
    }

    generateDestFileLoc(srcFileLoc: SourceFileLocation) {
        if (!srcFileLoc) {
            return null;
        }
        const destFileLoc = {
            relativePath: srcFileLoc.relativePath
        } as SourceFileLocation;
        const srcBaseDir = '' + srcFileLoc.baseDir;
        let destSubDirName = this._destSubDirNameMap.get(srcBaseDir);
        if (!destSubDirName) {
            destSubDirName = myutils.generateValidFileName(srcBaseDir);
            destSubDirName = myutils.modifyNameToBeAbsent(
                Array.from(this._destSubDirNameMap.values()),
                destSubDirName);
            this._destSubDirNameMap.set(srcBaseDir, destSubDirName);
        }
        destFileLoc.baseDir = destSubDirName;
        return destFileLoc;
    }

    async execute(): Promise<boolean> {
        const getFileContent = this.getFileContent || (() => {
            throw new Error("getFileContent property is not callable");
        });

        let codeChangeDetected = false;

        // reset
        this._destSubDirNameMap = new Map<string, string>();

        let itemIdx = -1; // let indices start from zero.
        for await (const sourceFileDescriptor of (this.srcFileDescriptors || [])) {
            itemIdx++;
            try {
                // validate srcPath.
                const srcFileLoc = this.callFunc(this.normalizeSrcFileLoc,
                    sourceFileDescriptor.baseDir, sourceFileDescriptor.relativePath);
                const srcPath = this.callFunc(this.stringifySrcFileLoc, srcFileLoc);

                // determine encoding to use.
                const srcFileEncoding =
                    sourceFileDescriptor.encoding || this.defaultEncoding || "utf8";

                // always generate files if code change detection is disabled.
                let generateDestFile  = true;
                let originalContent = '';
                const revisedContent = sourceFileDescriptor.content;
                const revisedBinaryContent = sourceFileDescriptor.binaryContent;
                const isBinary = sourceFileDescriptor.isBinary;
                if (this.codeChangeDetectionEnabled) {
                    if (isBinary) {
                        const originalBinaryContent = await this.callFunc(getFileContent,
                            srcFileLoc, true, null);
                        generateDestFile = !this.callFunc(this.areFileContentsEqual,
                            this, originalBinaryContent, revisedBinaryContent, true);
                    }
                    else {
                        originalContent = await this.callFunc(getFileContent,
                            srcFileLoc, false, srcFileEncoding);
                        generateDestFile = !this.callFunc(this.areFileContentsEqual,
                            this, originalContent, revisedContent, false);
                    }
                    if (generateDestFile) {
                        codeChangeDetected = true;
                    }
                }

                let destPath;
                if (generateDestFile) {
                    const destFileLoc = this.callFunc(this.generateDestFileLoc, srcFileLoc);
                    destPath = this.callFunc(this.stringifyDestFileLoc, destFileLoc);

                    if (isBinary) {
                        await this.callFunc(this.saveFileContent, destFileLoc,
                            revisedBinaryContent, true);
                    }
                    else {
                        await this.callFunc(this.saveFileContent, destFileLoc,
                            revisedContent, false, srcFileEncoding);
                    }
                }

                // write out possibly ungenerated destPath.
                await this.callFunc(this.appendOutputSummary, srcPath + os.EOL +
                    destPath + os.EOL);

                if (this.codeChangeDetectionEnabled && generateDestFile) {
                    // write out same info as output summary.
                    await this.callFunc(this.appendChangeSummary, srcPath + os.EOL +
                        destPath + os.EOL);

                    const appendChangeDetails = this.appendChangeDetails;
                    if (appendChangeDetails) {
                        // write out Unix normal diff of code changes.
                        const changeDiff = [""];
                        changeDiff.push(`${os.EOL}--- ${srcPath}${os.EOL}+++ ${destPath}${os.EOL}`);
                        if (isBinary) {
                            changeDiff.push(os.EOL + "Binary files differ" + os.EOL);
                        }
                        else {
                            const original = myutils.splitIntoLines('' + originalContent, false);
                            const revised = myutils.splitIntoLines('' + revisedContent, false);
                            changeDiff.push(myutils.printNormalDiff(original, revised));
                            await this.callFunc(appendChangeDetails, changeDiff.join(""));
                        }
                    }
                }
            }
            catch (e) {
                const logger = this.reportError;
                if (logger) {
                    await this.callFunc(logger, e, itemIdx + ":" +
                        "error encountered during processing of item");
                }
                else {
                    throw e;
                }
            }
        }

        return codeChangeDetected;
    }

    private callFunc(f: any, ...args: any) {
        if (f) {
            return f.call(this, ...args);
        }
    }
}
