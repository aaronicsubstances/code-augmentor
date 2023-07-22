import fs from "fs/promises";
import os from "os"
import path from "path";

import * as myutils from "./myutils";
import {
    SourceFileDescriptor
} from "./types";

/**
 * Name of file used to store list of files which were not skipped.
 */
const OUTPUT_SUMMARY_FILE_NAME = "OUTPUT-SUMMARY.txt";

/**
 * Name of file used to store list of changed files.
 */
const CHANGE_SUMMARY_FILE_NAME = "CHANGE-SUMMARY.txt";

/**
 * Name of file used to store diff of changed files.
 */
const CHANGE_DETAILS_FILE_NAME = "CHANGE-DETAILS.txt";

export default class CodeChangeDetective {
    codeChangeSupplier: AsyncIterable<SourceFileDescriptor>
        | Iterable<SourceFileDescriptor>
        | null = null;
    codeChangeProcessingErrorLog: ((e: any, m: string) => Promise<void>) | null = null;
    destDir = '';
    cleanDestDir = true;
    codeChangeDetectionDisabled = false;
    defaultEncoding: BufferEncoding | null = null;
    outputSummaryPath = '';
    changeSummaryPath = '';
    changeDetailsPath = '';

    async execute() {
        // validate required properties
        const codeChangeSupplier = this.codeChangeSupplier;
        if (!codeChangeSupplier) {
            throw new Error("codeChangeSupplier property is not set");
        }

        const codeChangeDetectionDisabled = this.codeChangeDetectionDisabled;

        let outputSummaryPath = this.outputSummaryPath;
        let changeDetailsPath = '';
        let changeSummaryPath = '';
        if (!codeChangeDetectionDisabled) {
            changeDetailsPath = this.changeDetailsPath;
            changeSummaryPath = this.changeSummaryPath;
        }

        // set defaults if destDir is set.
        const destDir = this.destDir;
        if (destDir) {
            if (!outputSummaryPath) {
                outputSummaryPath = path.join(destDir, OUTPUT_SUMMARY_FILE_NAME);
            }
            if (!codeChangeDetectionDisabled) {
                if (!changeDetailsPath) {
                    changeDetailsPath = path.join(destDir, CHANGE_DETAILS_FILE_NAME);
                }
                if (!changeSummaryPath) {
                    changeSummaryPath = path.join(destDir, CHANGE_SUMMARY_FILE_NAME);
                }
            }
        }

        // clean destination directory.
        if (destDir && this.cleanDestDir) {
            await fs.rm(destDir, { recursive: true, force: true});
            await fs.mkdir(destDir, { recursive: true });
        }

        let outputSummaryWriter = null;
        let changeSummaryWriter = null;
        let changeDiffWriter = null;

        let codeChangeDetected = false;

        try {
            if (outputSummaryPath) {
                outputSummaryWriter = await fs.open(outputSummaryPath, "w");
            }
            if (changeSummaryPath) {
                changeSummaryWriter = await fs.open(changeSummaryPath, "w");
            }
            if (changeDetailsPath) {
                changeDiffWriter = await fs.open(changeDetailsPath, "w");
            }

            const destSubDirNameMap = new Map<string, string>();
            let itemIdx = -1; // let indices start from zero.
            for await (const sourceFileDescriptor of codeChangeSupplier) {
                itemIdx++;
                try {
                    // validate srcPath and ensure it can be opened.
                    const srcFileLoc = myutils.normalizeSrcFileLoc(
                        sourceFileDescriptor.baseDir, sourceFileDescriptor.relativePath);
                    const srcPath = path.join(srcFileLoc.baseDir, srcFileLoc.relativePath);
                    await closeFd(await fs.open(srcPath));

                    // determine encoding to use.
                    const srcFileEncoding = 
                        sourceFileDescriptor.encoding || this.defaultEncoding || "utf8";

                    // always generate files if code change detection is disabled.
                    let srcFileUnchanged  = false;
                    let originalContent = '';
                    if (!codeChangeDetectionDisabled) {
                        if (sourceFileDescriptor.binaryContent) {
                            const originalBinaryContent = await fs.readFile(srcPath);
                            srcFileUnchanged = Buffer.compare(sourceFileDescriptor.binaryContent,
                                originalBinaryContent) === 0;
                        }
                        else {
                            originalContent = await fs.readFile(srcPath, srcFileEncoding);
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
                        await fs.mkdir(path.dirname(destPath), { recursive: true });
    
                        if (sourceFileDescriptor.binaryContent) {
                            await fs.writeFile(destPath, sourceFileDescriptor.binaryContent);
                        }
                        else {
                            await fs.writeFile(destPath, sourceFileDescriptor.content,
                                srcFileEncoding);
                        }
                    }
    
                    // add to output summary.
                    if (outputSummaryWriter) {
                        // write out possibly empty destPath.
                        await outputSummaryWriter.write(srcPath + os.EOL +
                            destPath + os.EOL);
                    }
    
                    if (changeSummaryWriter && destPath) {
                        // write out same info as output summary.
                        await changeSummaryWriter.write(srcPath + os.EOL +
                            destPath + os.EOL);
                    }
    
                    if (changeDiffWriter && destPath) {
                        // write out Unix normal diff of code changes.
                        await changeDiffWriter.write(
                            `${os.EOL}--- ${srcPath}${os.EOL}+++ ${destPath}${os.EOL}`);
                        if (sourceFileDescriptor.binaryContent) {
                            await changeDiffWriter.write(os.EOL + "Binary files differ" + os.EOL);
                        }
                        else {
                            if (!originalContent) {
                                originalContent = await fs.readFile(srcPath, srcFileEncoding);
                            }
                            const original = myutils.splitIntoLines(originalContent, false);
                            const revised = myutils.splitIntoLines(sourceFileDescriptor.content, false);
                            const changeDiff = myutils.printNormalDiff(original, revised);
                            await changeDiffWriter.write(changeDiff);
                        }
                    }
                }
                catch (e) {
                    const logger = this.codeChangeProcessingErrorLog;
                    if (logger) {
                        logger(e, "Error encountered during processing of item " +
                            itemIdx);
                    }
                    else {
                        throw e;
                    }
                }
            }

            return codeChangeDetected;
        }
        finally {
            await closeFd(outputSummaryWriter);
            await closeFd(changeSummaryWriter);
            await closeFd(changeDiffWriter);
        }
    }
}

async function closeFd(fd: any) {
    if (fd) {
        return new Promise<void>((resolve, reject) => {
            fd.close((err: any) => {
                if (err) {
                    reject(err);
                }
                else {
                    resolve();
                }
            });
        });
    }
}
