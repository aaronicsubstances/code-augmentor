import fs from "fs/promises";
import path from "path";
import os from "os";

import * as myutils from "./myutils";
import {
    CodeChangeDetectiveConfig,
    CodeChangeDetectiveConfigFactory,
    SourceFileDescriptor,
    SourceFileLocation
} from "./types";

export class CodeChangeDetective {
    codeChangeDetectionEnabled = true;
    configFactory?: CodeChangeDetectiveConfigFactory = new DefaultCodeChangeDetectiveConfigFactory()
    srcFileDescriptors?: AsyncIterable<SourceFileDescriptor>
        | Iterable<SourceFileDescriptor>;
    defaultEncoding?: BufferEncoding;
    reportError?: (e: any, m: string) => Promise<void>;

    async execute(): Promise<boolean> {
        let config;
        try {
            let codeChangeDetected = false;

            let itemIdx = -1; // let indices start from zero.
            for await (const sourceFileDescriptor of (this.srcFileDescriptors || [])) {
                itemIdx++;
                if (!sourceFileDescriptor) {
                    continue;
                }
                if (!config) {
                    const configFactory = this.configFactory;
                    if (!configFactory) {
                        throw new Error("configFactory property is not set");
                    }
                    config = await configFactory.create();
                    if (!config) {
                        throw new Error("configFactory property returned null config");
                    }
                }
                try {
                    // validate srcPath.
                    const srcFileLoc = config.normalizeSrcFileLoc(sourceFileDescriptor);
                    const srcPath = config.stringifySrcFileLoc(srcFileLoc);

                    // determine encoding to use.
                    const srcFileEncoding =
                        sourceFileDescriptor.encoding || this.defaultEncoding || "utf8";

                    // always generate files if code change detection is disabled.
                    let generateDestFile  = true;
                    let originalContent: string | Buffer | undefined;
                    const revisedContent = sourceFileDescriptor.content as string;
                    const revisedBinaryContent = sourceFileDescriptor.binaryContent as Buffer;
                    const isBinary = sourceFileDescriptor.isBinary;
                    if (this.codeChangeDetectionEnabled) {
                        if (isBinary) {
                            const originalBinaryContent = await config.getFileContent(
                                srcFileLoc, true);
                            generateDestFile = !config.areFileContentsEqual(
                                originalBinaryContent, revisedBinaryContent, true);
                        }
                        else {
                            originalContent = await config.getFileContent(
                                srcFileLoc, false, srcFileEncoding);
                            generateDestFile = !config.areFileContentsEqual(
                                originalContent, revisedContent, false);
                        }
                        if (generateDestFile) {
                            codeChangeDetected = true;
                        }
                    }

                    let destPath = '';
                    if (generateDestFile) {
                        const destFileLoc = config.generateDestFileLoc(srcFileLoc);
                        destPath = config.stringifyDestFileLoc(destFileLoc);

                        if (isBinary) {
                            await config.saveFileContent(destFileLoc,
                                revisedBinaryContent, true);
                        }
                        else {
                            await config.saveFileContent(destFileLoc,
                                revisedContent, false, srcFileEncoding);
                        }
                    }

                    // write out possibly ungenerated destPath.
                    await callFunc(config.appendOutputSummary, config,
                        srcPath + os.EOL + destPath + os.EOL);

                    if (this.codeChangeDetectionEnabled && generateDestFile) {
                        // write out same info as output summary into change summary.
                        await callFunc(config.appendChangeSummary, config,
                            srcPath + os.EOL + destPath + os.EOL);

                        const appendChangeDiff = config.appendChangeDiff;
                        if (appendChangeDiff) {
                            // write out Unix normal diff of code changes.
                            const changeDiff = [""];
                            changeDiff.push(`${os.EOL}--- ${srcPath}${os.EOL}+++ ${destPath}${os.EOL}`);
                            if (isBinary) {
                                changeDiff.push(`${os.EOL}Binary files differ${os.EOL}`);
                            }
                            else {
                                const original = myutils.splitIntoLines('' + originalContent, false);
                                const revised = myutils.splitIntoLines('' + revisedContent, false);
                                changeDiff.push(myutils.printNormalDiff(original, revised));
                            }
                            await callFunc(appendChangeDiff, config, changeDiff.join(""));
                        }
                    }
                }
                catch (e) {
                    const logger = this.reportError;
                    if (logger) {
                        await callFunc(logger, this, e, itemIdx + ":" +
                            "error encountered during processing of item");
                    }
                    else {
                        throw e;
                    }
                }
            }

            return codeChangeDetected;
        }
        finally {
            await callFunc(config?.release, config);
        }
    }
}

function callFunc(f: any, ...args: any) {
    if (f) {
        return f.call(...args);
    }
}

export class DefaultCodeChangeDetectiveConfigFactory implements CodeChangeDetectiveConfigFactory {
    destDir: string = '';
    cleanDestDir: boolean = true;

    async create() {
        const destDir = this.destDir;
        if (!destDir) {
            throw new Error("destDir property is null or invalid directory name");
        }
        if (this.cleanDestDir) {
            await myutils.cleanDir(destDir)
        }

        const config = new DefaultCodeChangeDetectiveConfig();
        config.destDir = destDir;
        return config;
    }
}

export class DefaultCodeChangeDetectiveConfig implements CodeChangeDetectiveConfig {
    destDir = '';
    private _destSubDirNameMap = new Map<string, string>();
    private _outputSummaryFileHandle?: fs.FileHandle;
    private _changeSummaryFileHandle?: fs.FileHandle;
    private _changeDiffFileHandle?: fs.FileHandle;
    private _released = false;

    async release(): Promise<void> {
        this._released = true;
        await this._outputSummaryFileHandle?.close();
        await this._changeSummaryFileHandle?.close();
        await this._changeDiffFileHandle?.close();
    }
    
    async appendOutputSummary(data: string): Promise<void> {
        if (this._released) {
            throw new Error("config has been released")
        }
        let writer = this._outputSummaryFileHandle;
        if (!writer) {
            writer = await fs.open(path.join(this.destDir, "output-summary.txt"), "w");
            this._outputSummaryFileHandle = writer;
        }
        await writer.write(data);
    }

    async appendChangeSummary(data: string): Promise<void> {
        if (this._released) {
            throw new Error("config has been released")
        }
        let writer = this._changeSummaryFileHandle;
        if (!writer) {
            writer = await fs.open(path.join(this.destDir, "change-summary.txt"), "w");
            this._changeSummaryFileHandle = writer;
        }
        await writer.write(data);
    }

    async appendChangeDiff(data: string): Promise<void> {
        if (this._released) {
            throw new Error("config has been released")
        }
        let writer = this._changeDiffFileHandle;
        if (!writer) {
            writer = await fs.open(path.join(this.destDir, "change-diff.txt"), "w");
            this._changeDiffFileHandle = writer;
        }
        await writer.write(data);
    }

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
        const p = path.join(this.destDir, loc.baseDir, loc.relativePath);
        await fs.mkdir(path.dirname(p), { recursive: true });
        if (isBinary) {
            await fs.writeFile(p, data);
        }
        else {
            await fs.writeFile(p, data, encoding);
        }
    }

    normalizeSrcFileLoc(srcFileDescriptor: SourceFileDescriptor) {
        return myutils.normalizeSrcFileLoc(srcFileDescriptor.baseDir as any,
            srcFileDescriptor.relativePath);
    }

    stringifySrcFileLoc(loc: SourceFileLocation) {
        return path.resolve(loc.baseDir, loc.relativePath);
    }

    stringifyDestFileLoc(loc: SourceFileLocation) {
        return path.resolve(this.destDir, loc.baseDir, loc.relativePath);
    }

    areFileContentsEqual(arg1: any, arg2: any, isBinary: boolean) {
        if (isBinary) {
            return Buffer.compare(arg1, arg2) === 0;
        }
        else {
            return arg1 === arg2;
        }
    }

    generateDestFileLoc(srcFileLoc: SourceFileLocation): SourceFileLocation {
        let destSubDirName = this._destSubDirNameMap.get(srcFileLoc.baseDir);
        if (!destSubDirName) {
            destSubDirName = myutils.generateValidFileName(srcFileLoc.baseDir);
            destSubDirName = myutils.modifyNameToBeAbsent(
                Array.from(this._destSubDirNameMap.values()),
                destSubDirName);
            this._destSubDirNameMap.set(srcFileLoc.baseDir, destSubDirName);
        }
        const destFileLoc = {
            baseDir: destSubDirName,
            relativePath: srcFileLoc.relativePath
        };
        return destFileLoc;
    }
}