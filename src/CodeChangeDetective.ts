import fs from "fs/promises";
import os from "os"
import path from "path";

import * as myutils from "./myutils";
import {
    SourceFileDescriptor
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

    async getFileContent(p: string, encoding?: BufferEncoding) {
        if (encoding) {
            return await fs.readFile(p, encoding);
        }
        else {
            return await fs.readFile(p);
        }
    }

    async saveFileContent(p: string, data: string | Buffer, encoding?: BufferEncoding) {
        await fs.mkdir(path.dirname(p), { recursive: true });
        if (encoding) {
            await fs.writeFile(p, data, encoding);
        }
        else {
            await fs.writeFile(p, data);
        }
    }

    async defaultSetup(destDir: string, cleanDestDir: boolean = true) {
        if (!destDir) {
            throw new Error("destDir argument is null or invalid directory name");
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

    async execute(): Promise<boolean> {
        const srcFileDescriptors = this.srcFileDescriptors || [];
        const getFileContent = this.getFileContent || (() => {
            throw new Error("getFileContent property is not callable");
        });

        const codeChangeDetectionEnabled = this.codeChangeDetectionEnabled;

        const destDir = this.destDir;

        let codeChangeDetected = false;

        const destSubDirNameMap = new Map<string, string>();
        let itemIdx = -1; // let indices start from zero.
        for await (const sourceFileDescriptor of srcFileDescriptors) {
            itemIdx++;
            try {
                // validate srcPath.
                const srcFileLoc = myutils.normalizeSrcFileLoc(
                    sourceFileDescriptor.baseDir, sourceFileDescriptor.relativePath);
                const srcPath = path.join(srcFileLoc.baseDir, srcFileLoc.relativePath);

                // determine encoding to use.
                const srcFileEncoding =
                    sourceFileDescriptor.encoding || this.defaultEncoding || "utf8";

                // always generate files if code change detection is disabled.
                let srcFileUnchanged  = false;
                let originalContent = '';
                if (codeChangeDetectionEnabled) {
                    if (sourceFileDescriptor.binaryContent) {
                        const originalBinaryContent = await this.callAsyncFunc(getFileContent,
                            srcPath) as Buffer;
                        srcFileUnchanged = Buffer.compare(sourceFileDescriptor.binaryContent,
                            originalBinaryContent) === 0;
                    }
                    else {
                        originalContent = await this.callAsyncFunc(getFileContent,
                            srcPath, srcFileEncoding) as string;
                        srcFileUnchanged = sourceFileDescriptor.content === originalContent;
                    }
                    if (!srcFileUnchanged) {
                        codeChangeDetected = true;
                    }
                }

                let destPath = '';
                if (destDir && !srcFileUnchanged) {
                    let destSubDirName = destSubDirNameMap.get(srcFileLoc.baseDir);
                    if (!destSubDirName) {
                        destSubDirName = myutils.generateValidFileName(srcFileLoc.baseDir);
                        destSubDirName = myutils.modifyNameToBeAbsent(
                            Array.from(destSubDirNameMap.values()),
                            destSubDirName);
                        destSubDirNameMap.set(srcFileLoc.baseDir, destSubDirName);
                    }
                    destPath = path.join(destDir, destSubDirName,
                        srcFileLoc.relativePath);

                    if (sourceFileDescriptor.binaryContent) {
                        await this.callAsyncFunc(this.saveFileContent, destPath,
                            sourceFileDescriptor.binaryContent);
                    }
                    else {
                        await this.callAsyncFunc(this.saveFileContent, destPath,
                            sourceFileDescriptor.content, srcFileEncoding);
                    }
                }

                // write out possibly empty destPath.
                await this.callAsyncFunc(this.appendOutputSummary, srcPath + os.EOL +
                    destPath + os.EOL);

                if (destPath) {
                    // write out same info as output summary.
                    await this.callAsyncFunc(this.appendChangeSummary, srcPath + os.EOL +
                        destPath + os.EOL);

                    const appendChangeDetails = this.appendChangeDetails;
                    if (appendChangeDetails) {
                        // write out Unix normal diff of code changes.
                        const changeDiff = [""];
                        changeDiff.push(`${os.EOL}--- ${srcPath}${os.EOL}+++ ${destPath}${os.EOL}`);
                        if (sourceFileDescriptor.binaryContent) {
                            changeDiff.push(os.EOL + "Binary files differ" + os.EOL);
                        }
                        else {
                            if (!originalContent) {
                                originalContent = await this.callAsyncFunc(getFileContent,
                                    srcPath, srcFileEncoding) as string;
                            }
                            const original = myutils.splitIntoLines(originalContent, false);
                            const revised = myutils.splitIntoLines(sourceFileDescriptor.content, false);
                            changeDiff.push(myutils.printNormalDiff(original, revised));
                            await this.callAsyncFunc(appendChangeDetails, changeDiff.join(""));
                        }
                    }
                }
            }
            catch (e) {
                const logger = this.reportError;
                if (logger) {
                    await this.callAsyncFunc(logger, e,
                        "Error encountered during processing of item " + itemIdx);
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
