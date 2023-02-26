const BLANK_START_PATTERN = new RegExp("^\\s*");

function splitIntoLines(text, separateTerminators) {
    const splitText = [];
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

function locateNewline(content, start, receipt) {
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

function isBlank(s) {
    if (!s) {
        return true;
    }
    return determineIndent(s).length == s.length;
}

function determineIndent(value) {
    const m = BLANK_START_PATTERN.exec(value);
    return m[0];
}

/**
 * Generates normal diff output in "normal" format, ie neither context format nor
 * unified format. Aims to mimick Unix diff command exactly.
 * @param x lines in original file. Each line should include its terminator. 
 * @param y lines in revised file. Each line should include its terminator.
 * @returns Unix normal diff
 */
function printNormalDiff(x, y) {
    /*
     * The following resources were used for implementation:
     * <ul>
     *   <li>https://introcs.cs.princeton.edu/java/23recursion/ ,
     *   <li>https://introcs.cs.princeton.edu/java/23recursion/Diff.java.html ,
     *   <li>https://introcs.cs.princeton.edu/java/23recursion/LongestCommonSubsequence.java.html
     *   <li>https://www.gnu.org/software/diffutils/manual/html_node/Detailed-Normal.html
     * </ul>
     */
    const diffCollector = [];

    // number of lines of each file
    const m = x.length;
    const n = y.length;

    // opt[i][j] = length of LCS of x[i..m] and y[j..n]
    //int[][] opt = new int[m+1][n+1];
    const opt = [];
    for (let i = 0; i < m + 1; i++) {
        const optChild = [];
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

function printDiffLines(diffCollector, xLines, yLines,
        xLineRange, yLineRange) {
    if (!xLines.length && !yLines.length) {
        return;
    }
    if (!xLines.length) {
        // insertion
        diffCollector.push(xLineRange[0]);
        diffCollector.push("a");
        stringifyRange(yLineRange, diffCollector);
        diffCollector.push("\n");
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
        diffCollector.push("\n");
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
        diffCollector.push("\n");
        for (const x of xLines) {
            diffCollector.push("< ");
            formatLine(x, diffCollector);
        }
        diffCollector.push("---\n");
        for (const y of yLines) {
            diffCollector.push("> ");
            formatLine(y, diffCollector);
        }
    }
}

function stringifyRange(range, diffCollector) {
    if (range[0] === range[1]) {
        diffCollector.push(range[0] + 1);
    }
    else {
        diffCollector.push(range[0] + 1);
        diffCollector.push(",");
        diffCollector.push(range[1] + 1);
    }
}

function formatLine(line, diffCollector) {
    const newlineSuffixLen = findNewlineSuffixLen(line);
    if (newlineSuffixLen > 0) {
        diffCollector.push(line.substring(0, line.length() - newlineSuffixLen));
        diffCollector.push("\n");
    }
    else {
        diffCollector.push(line);
        diffCollector.push("\n");
        diffCollector.push("\\ No newline at end of file");
        diffCollector.push("\n");
    }
}

function findNewlineSuffixLen(line) {
    if (line.endsWith("\r\n")) {
        return 2;
    }
    if (line.endsWith("\n") || line.endsWith("\r")) {
        return 1;
    }
    return 0;
}

module.exports = {
    splitIntoLines,
    isBlank,
    determineIndent,
    printNormalDiff
};