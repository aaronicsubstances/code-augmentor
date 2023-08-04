import fs from "fs/promises";
import os from "os";
import path from "path";
import { SourceFileLocation } from "./types";

const BLANK_START_PATTERN = new RegExp("^\\s*");
const FILE_NAME_VALIDITY_CHECK_REGEX = new RegExp(/[^a-zA-Z0-9_-]/g);

export function splitIntoLines(text: string, separateTerminators: boolean) {
    const splitText = new Array<string>();
    let lastEndIdx = 0;
    const temp = [0, 0];
    while (true) {
        locateNewline(text, lastEndIdx, temp);
        let idx = temp[0];
        if (idx === -1) {
            break;
        }
        let newlineLen = temp[1];
        let endIdx = idx + newlineLen;
        if (separateTerminators) {
            const precedingLine = text.substring(lastEndIdx, idx);
            const terminatingNewline = text.substring(idx, endIdx);
            splitText.push(precedingLine);
            splitText.push(terminatingNewline);
        }
        else {
            const precedingLine = text.substring(lastEndIdx, endIdx);
            splitText.push(precedingLine);
        }
        lastEndIdx = endIdx;
    }
    if (lastEndIdx < text.length) {
        splitText.push(text.substring(lastEndIdx));
        if (separateTerminators) {
            splitText.push("");
        }
    }
    return splitText;
}

function locateNewline(content: string, start: number, receipt: number[]) {
    let winLn = false;
    let idx = content.indexOf("\r\n", start);
    if (idx != -1) {
        winLn = true;
    }
    let idx2 = content.indexOf('\n', start);
    if (idx == -1) {
        idx = idx2;
        winLn = false;
    }
    else if (idx2 != -1 && idx2 < idx) {
        idx = idx2;
        winLn = false;
    }
    let idx3 = content.indexOf('\r', start);
    if (idx == -1) {
        idx = idx3;
        winLn = false;
    }
    else if (idx3 != -1 && idx3 < idx) {
        idx = idx3;
        winLn = false;
    }
    receipt[0] = idx;
    receipt[1] = winLn ? 2 : 1;
}

export function determineIndent(value: string): string {
    const m = BLANK_START_PATTERN.exec(value);
    return m![0];
}

export function isBlank(s: string | null) {
    if (!s) {
        return true;
    }
    return determineIndent(s).length == s.length;
}

/**
 * Generates a name with a given prefix which is guaranteed to be absent in a given list. If 
 * prefix is not in the list in the first place, then prefix is simply returned.
 * @param names given list.
 * @param originalName given prefix.
 * @returns a name which has originalName as a prefix and is not in names list.
 */
export function modifyNameToBeAbsent(names: string[], originalName: string) {
    if (!names.includes(originalName)) {
        return originalName;
    }
    let index = 1;
    let modifiedName = originalName + "-" + index;
    while (names.includes(modifiedName)) {
        if (index < 9) {
            index++;
            modifiedName = originalName + "-" + index;
        }
        else {
            // out of desperation try appending random integers containing 8 digits.
            modifiedName = originalName + "-" + getRndInteger(10000000, 100000000);
        }
    }
    return modifiedName;
}

/**
     * Generates a name with a given prefix which is guaranteed to be absent in a given list of strings. If 
     * prefix is not in any of the strings in the first place, then prefix is simply returned.
     * @param target given list.
     * @param originalText given prefix.
     * @returns a name which has originalText as a prefix and is not a substring of any string in target strings.
     */
export function modifyTextToBeAbsent(target: string[], originalText: string) {
    if (!target.some(x => x.indexOf(originalText) != -1)) {
        return originalText;
    }
    let index = 1;
    let modifiedName = originalText + "-" + index;
    while (target.some(x => x.indexOf(modifiedName) != -1)) {
        if (index < 9) {
            index++;
            modifiedName = originalText + "-" + index;
        }
        else {
            // out of desperation try appending random integers containing 8 digits.
            modifiedName = originalText + "-" + getRndInteger(10000000, 100000000);
        }
    }
    return modifiedName;
}

/**
 * This JavaScript function always returns a random number between min (included) and max (excluded).
 * (copied from https://www.w3schools.com/js/js_random.asp).
 * @param min 
 * @param max 
 * @returns 
 */
function getRndInteger(min: number, max: number) {
    return Math.floor(Math.random() * (max - min) ) + min;
}

/**
 * NB: exported for internal use only.
 */
export function _generateValidFileName(p: string) {
    let trimmed = "";
    if (p) {
        // use last path segment
        const name = path.basename(p);
        trimmed = name.replace(FILE_NAME_VALIDITY_CHECK_REGEX, "");
    }
    if (!trimmed) {
        return "c";
    }
    return trimmed;
}

export function normalizeSrcFileLoc(baseDir: string | null, relativePath: string) {
    if (!relativePath) {
        throw new Error("no relative (or absolute) path provided");
    }
    let fullPath;
    if (baseDir) {
        baseDir = path.resolve(baseDir);
        fullPath = path.resolve(baseDir, relativePath);
    }
    else {
        fullPath = path.resolve(relativePath);
    }
    return _splitFilePath(fullPath, baseDir);
}

/**
 * NB: exported for testing only.
 */
export function _splitFilePath(fullPath: string, baseDir: string | null) {
    // ensure prescence of file separator.
    let lastSlashIdx = fullPath.lastIndexOf("/");
    if (lastSlashIdx === -1) {
        lastSlashIdx = fullPath.lastIndexOf("\\");
    }
    if (lastSlashIdx === -1) {
        throw new Error("missing slash in regular file path argument: " +
            fullPath);
    }
    if (baseDir && fullPath.length > baseDir.length &&
            fullPath.startsWith(baseDir)) {
        // remove leading slash from relative path.
        const relativePath = fullPath.substring(baseDir.length + 1);
        const ret: SourceFileLocation = {
            baseDir, relativePath
        };
        return ret;
    }
    else {
        // construct new base dir but keep trailing slash in it,
        // in order to cater for MS Windows case where root folders
        // have trailing slashes (e.g. C:\, D:\).
        baseDir  = fullPath.substring(0, lastSlashIdx + 1);
        const relativePath = fullPath.substring(lastSlashIdx + 1);
        const ret: SourceFileLocation = {
            baseDir, relativePath
        };
        return ret;
    }
}

export async function cleanDir(d: string) {
    await fs.rm(d, { recursive: true, force: true });
    await fs.mkdir(d, { recursive: true });
}

/**
 * Generates normal diff output in "normal" format, ie neither context format nor
 * unified format. Aims to mimick Unix diff command exactly.
 * @param x lines in original file. Each line should include its terminator. 
 * @param y lines in revised file. Each line should include its terminator.
 * @returns Unix normal diff
 */
export function printNormalDiff(x: string[], y: string[]) {
    /*
     * The following resources were used for implementation:
     * <ul>
     *   <li>https://introcs.cs.princeton.edu/java/23recursion/ ,
     *   <li>https://introcs.cs.princeton.edu/java/23recursion/Diff.java.html ,
     *   <li>https://introcs.cs.princeton.edu/java/23recursion/LongestCommonSubsequence.java.html
     *   <li>https://www.gnu.org/software/diffutils/manual/html_node/Detailed-Normal.html
     * </ul>
     */
    const diffCollector = new Array<any>();

    // number of lines of each file
    const m = x.length;
    const n = y.length;

    // opt[i][j] = length of LCS of x[i..m] and y[j..n]
    //int[][] opt = new int[m+1][n+1];
    const opt: number[][] = [];
    for (let i = 0; i < m + 1; i++) {
        const optChild: number[] = [];
        opt.push(optChild);
        for (let j = 0; j < n + 1; j++) {
            optChild.push(0);
        }
    }

    // compute length of LCS and all subproblems via dynamic programming
    for (let i = m-1; i >= 0; i--) {
        for (let j = n-1; j >= 0; j--) {
            if (x[i] === y[j])
                opt[i][j] = opt[i+1][j+1] + 1;
            else 
                opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
        }
    }

    // recover LCS itself and print out non-matching lines to standard output
    let i = 0, j = 0;
    const xLines = [];
    const yLines = [];
    const xLineRange = [0, 0];
    const yLineRange = [0, 0];
    while (i < m && j < n) {
        if (x[i] === y[j]) {
            printDiffLines(diffCollector, xLines, yLines, xLineRange, yLineRange);
            xLines.length = 0; // xLines.clear()
            yLines.length = 0; // yLines.clear()
            
            xLineRange[0] = ++i;
            yLineRange[0] = ++j;
        }
        else if (opt[i+1][j] >= opt[i][j+1]) {
            xLineRange[1] = i;
            xLines.push(x[i++]);
        }
        else {
            yLineRange[1] = j;
            yLines.push(y[j++]);
        }
    }
    
    while (i < m) {
        xLineRange[1] = i;
        xLines.push(x[i++]);
    }
    
    while (j < n) {
        yLineRange[1] = j;
        yLines.push(y[j++]);
    }
    
    printDiffLines(diffCollector, xLines, yLines, xLineRange, yLineRange);
    return diffCollector.join("");
}

function printDiffLines(diffCollector: any[], xLines: string[], yLines: string[],
        xLineRange: number[], yLineRange: number[]) {
    if (!xLines.length && !yLines.length) {
        return;
    }
    if (!xLines.length) {
        // insertion
        diffCollector.push(xLineRange[0]);
        diffCollector.push("a");
        stringifyRange(yLineRange, diffCollector);
        diffCollector.push(os.EOL);
        for (const y of yLines) {
            diffCollector.push("> ");
            formatLine(y, diffCollector);
        }
    }
    else if (!yLines.length) {
        // deletion
        stringifyRange(xLineRange, diffCollector);
        diffCollector.push("d");
        diffCollector.push(yLineRange[0]);
        diffCollector.push(os.EOL);
        for (const x of xLines) {
            diffCollector.push("< ");
            formatLine(x, diffCollector);
        }
    }
    else {
        // change.
        stringifyRange(xLineRange, diffCollector);
        diffCollector.push("c");
        stringifyRange(yLineRange, diffCollector);
        diffCollector.push(os.EOL);
        for (const x of xLines) {
            diffCollector.push("< ");
            formatLine(x, diffCollector);
        }
        diffCollector.push("---");
        diffCollector.push(os.EOL);
        for (const y of yLines) {
            diffCollector.push("> ");
            formatLine(y, diffCollector);
        }
    }
}

function stringifyRange(range: number[], diffCollector: any[]) {
    if (range[0] === range[1]) {
        diffCollector.push(range[0] + 1);
    }
    else {
        diffCollector.push(range[0] + 1);
        diffCollector.push(",");
        diffCollector.push(range[1] + 1);
    }
}

function formatLine(line: string, diffCollector: any[]) {
    const newlineSuffixLen = findNewlineSuffixLen(line);
    if (newlineSuffixLen > 0) {
        diffCollector.push(line.substring(0, line.length - newlineSuffixLen));
    }
    else {
        diffCollector.push(line);
        diffCollector.push(os.EOL);
        diffCollector.push("\\ No newline at end of file");
    }
    diffCollector.push(os.EOL);
}

function findNewlineSuffixLen(line: string): number {
    if (line.endsWith("\r\n")) {
        return 2;
    }
    if (line.endsWith("\n") || line.endsWith("\r")) {
        return 1;
    }
    return 0;
}
