const assert = require('assert').strict;
const fs = require('fs');

const lineReader = require('line-reader');
const yargs = require('yargs');

// Import modules with methods for generating code
const yestest = require('./yestest');

const argv = yargs
    .option('input-path', {
        alias: 'i',
        description: 'Input file with augmenting code',
        type: 'string',
    })
    .nargs('input-path', 1)
    .option('output-path', {
        alias: 'o',
        description: 'Output file for generated code',
        type: 'string',
    })
    .nargs('output-path', 1)
    .demandOption(["input-path", "output-path"])
    .help()
    .alias('help', 'h')
    .argv;

// begin writing output file.
const writeStream = fs.createWriteStream(argv.outputPath);
// send header.
writeStream.write(JSON.stringify({}, '') + '\n');

const context = {
    globalScope: {}
};

// begin reading input file.
let firstLineSeen = false;
lineReader.eachLine(argv.inputPath, function(line, last) {
    let fileAugCodes = JSON.parse(line);
    // skip header.
    if (!firstLineSeen) {
        firstLineSeen = true;
        return;
    }
    // fetch arguments, and parse any json argument found.
    for (augCode of fileAugCodes.augmentingCodes) {
        augCode.args = [];
        for (block of augCode.blocks) {
            if (block.jsonify) {
                const parsedArg = JSON.parse(block.content);
                augCode.args.push(parsedArg);
            }
            else if (block.stringify) {
                augCode.args.push(block.content);
            }
        }
    }

    // set up context.
    const fileGenCodes = { 
        fileIndex: fileAugCodes.fileIndex,
        generatedCodes: []
    };
    context.fileScope = {};
    context.fileAugCodes = fileAugCodes;
    
    // now begin aug code processing.
    let i = 0;
    while (i < fileAugCodes.augmentingCodes.length) {
        const augCode = fileAugCodes.augmentingCodes[i];
        const functionName = augCode.blocks[0].content.trim();
        context.augCodeIndex = i;
        const genCodes = processAugCode(functionName, augCode, context);
        assert.ok(genCodes.length);
        for (let j = 0; j < genCodes.length; j++) {
            const genCode = genCodes[j];
            fileGenCodes.generatedCodes.push(genCode);
        }
        i += genCodes.length;
    }
    writeStream.write(JSON.stringify(fileGenCodes, '') + '\n');
    if (last) {
        writeStream.end();
    }
});

const FUNCTION_NAME_REGEX = /^((yestest)\.)[a-zA-Z]\w*$/;
function callUserFunction(functionName, augCode, context) {
    // validate name.
    if (!FUNCTION_NAME_REGEX.test(functionName)) {
        throw new Error("Invalid/Unsupported function name: " + functionName);
    }

    // name is valid. make function call "dynamically".
    const result = eval(functionName + "(augCode, context)");
    return result;
}

function processAugCode(functionName, augCode, context) {
    let result;
    try {
        result = callUserFunction(functionName, augCode, context);
    }
    catch (err) {
        return createErrorGenCode(context.augCodeIndex, err);
    }

    // validate return result: must be array or string.
    // also make all return results of array type.
    if (Array.isArray(result)) {
        if (result.length == 0) {
            return createErrorGenCode(context.augCodeIndex, "Received empty results");
        }
        for (let j = 0; j < result.length; j++) {
            const genCode = result[j];
            if (context.augCodeIndex + j >= context.fileAugCodes.length) {
                return createErrorGenCode(context.augCodeIndex, "No aug code found at offset " + j);
            }
            const correspondingAugCode = context.fileAugCodes.augmentingCodes[context.augCodeIndex + j];
            genCode.index = correspondingAugCode.index;
        }
    }
    else if (typeof result === 'string') {
        result = [{
            index: augCode.index,
            content: result
        }];
    }
    else {
        // error.
        if (result === null || typeof result === 'undefined') {
            return createErrorGenCode(context.augCodeIndex, 'Received no result')
        }
        else {
            return createErrorGenCode(context.augCodeIndex, 'Received unexpected result type: ' + typeof result);
        }
    }
    return result;
}

function createErrorGenCode(augCodeIndex, errOrMsg) {
    return [{
        index: augCodeIndex,
        error: true,
        body: errOrMsg.toString()
    }];
}