import fs from "fs/promises";
import os from "os"
import path from "path";

import * as myutils from "./myutils";
import {
    SourceFileDescriptor, SourceFileLocation
} from "./types";

export default class CodeChangeDetective {
    destDir = '';
    codeChangeDetectionEnabled = true;
    srcFileDescriptors?: AsyncIterable<SourceFileDescriptor>
        | Iterable<SourceFileDescriptor>;
    defaultEncoding?: BufferEncoding;
    appendOutputSummary?: (data: string) => Promise<void>;
    appendChangeSummary?: (data: string) => Promise<void>;
    appendChangeDetails?: (data: string) => Promise<void>;
    reportError?: (e: any, m: string) => Promise<void>;

    async getFileContent(loc: SourceFileLocation, encoding?: BufferEncoding) {
        const p = path.join(loc.baseDir, loc.relativePath);
        if (encoding) {
            return await fs.readFile(p, encoding);
        }
        else {
            return await fs.readFile(p);
        }
    }

    async saveFileContent(loc: SourceFileLocation, data: string | Buffer, encoding?: BufferEncoding) {
        const p = path.join(this.destDir || '', loc.baseDir, loc.relativePath);
        await fs.mkdir(path.dirname(p), { recursive: true });
        if (encoding) {
            await fs.writeFile(p, data, encoding);
        }
        else {
            await fs.writeFile(p, data);
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

    async execute(): Promise<boolean> {
        const getFileContent = this.getFileContent || (() => {
            throw new Error("getFileContent property is not callable");
        });

        let codeChangeDetected = false;

        const destSubDirNameMap = new Map<string, string>();
        let itemIdx = -1; // let indices start from zero.
        for await (const sourceFileDescriptor of (this.srcFileDescriptors || [])) {
            itemIdx++;
            try {
                // validate srcPath.
                const srcFileLoc = this.normalizeSrcFileLoc(
                    sourceFileDescriptor.baseDir, sourceFileDescriptor.relativePath);
                const srcPath = this.stringifySrcFileLoc(srcFileLoc);

                // determine encoding to use.
                const srcFileEncoding =
                    sourceFileDescriptor.encoding || this.defaultEncoding || "utf8";

                // always generate files if code change detection is disabled.
                let generateDestFile  = true;
                let originalContent = '';
                const revisedBinaryContent = sourceFileDescriptor.binaryContent;
                const revisedContent = sourceFileDescriptor.content;
                if (this.codeChangeDetectionEnabled) {
                    if (revisedBinaryContent) {
                        const originalBinaryContent = await this.callAsyncFunc(getFileContent,
                            srcFileLoc) as Buffer;
                        if (!originalBinaryContent) {
                            generateDestFile = true;
                        }
                        else {
                            generateDestFile = Buffer.compare(revisedBinaryContent,
                                originalBinaryContent) !== 0;
                        }
                    }
                    else {
                        originalContent = await this.callAsyncFunc(getFileContent,
                            srcFileLoc, srcFileEncoding) as string;
                        if (!revisedContent && !originalContent) {
                            generateDestFile = false;
                        }
                        else {
                            generateDestFile = revisedContent !== originalContent;
                        }
                    }
                    if (generateDestFile) {
                        codeChangeDetected = true;
                    }
                }

                let destPath = '';
                if (generateDestFile) {
                    let destSubDirName = destSubDirNameMap.get(srcFileLoc.baseDir);
                    if (!destSubDirName) {
                        destSubDirName = myutils.generateValidFileName(srcFileLoc.baseDir);
                        destSubDirName = myutils.modifyNameToBeAbsent(
                            Array.from(destSubDirNameMap.values()),
                            destSubDirName);
                        destSubDirNameMap.set(srcFileLoc.baseDir, destSubDirName);
                    }
                    const destFileLoc = {
                        baseDir: destSubDirName,
                        relativePath: srcFileLoc.relativePath
                    };
                    destPath = this.stringifyDestFileLoc(destFileLoc);

                    if (revisedBinaryContent) {
                        await this.callAsyncFunc(this.saveFileContent, destFileLoc,
                            revisedBinaryContent);
                    }
                    else {
                        await this.callAsyncFunc(this.saveFileContent, destFileLoc,
                            revisedContent, srcFileEncoding);
                    }
                }

                // write out possibly empty destPath.
                await this.callAsyncFunc(this.appendOutputSummary, srcPath + os.EOL +
                    destPath + os.EOL);

                if (this.codeChangeDetectionEnabled && generateDestFile) {
                    // write out same info as output summary.
                    await this.callAsyncFunc(this.appendChangeSummary, srcPath + os.EOL +
                        destPath + os.EOL);

                    const appendChangeDetails = this.appendChangeDetails;
                    if (appendChangeDetails) {
                        // write out Unix normal diff of code changes.
                        const changeDiff = [""];
                        changeDiff.push(`${os.EOL}--- ${srcPath}${os.EOL}+++ ${destPath}${os.EOL}`);
                        if (revisedBinaryContent) {
                            changeDiff.push(os.EOL + "Binary files differ" + os.EOL);
                        }
                        else {
                            if (!originalContent) {
                                originalContent = await this.callAsyncFunc(getFileContent,
                                    srcFileLoc, srcFileEncoding) as string;
                            }
                            let original = new Array<string>();
                            if (originalContent) {
                                original = myutils.splitIntoLines(originalContent, false);
                            }
                            let revised = new Array<string>();
                            if (revisedContent) {
                                revised = myutils.splitIntoLines(revisedContent, false);
                            }
                            changeDiff.push(myutils.printNormalDiff(original, revised));
                            await this.callAsyncFunc(appendChangeDetails, changeDiff.join(""));
                        }
                    }
                }
            }
            catch (e) {
                const logger = this.reportError;
                if (logger) {
                    await this.callAsyncFunc(logger, e, itemIdx + ":" +
                        "error encountered during processing of item");
                }
                else {
                    throw e;
                }
            }
        }

        return codeChangeDetected;
    }

    private async callAsyncFunc(f: any, ...args: any) {
        if (f) {
            return await f.call(this, ...args);
        }
    }
}
