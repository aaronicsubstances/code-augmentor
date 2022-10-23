const assert = require('assert').strict;
const fs = require('fs');
const readline = require('readline');
var endOfLine = require('os').EOL;
const path = require('path');
const stream = require('stream');
const util = require('util');

const ProcessCodeContext = require('./ProcessCodeContext');

// class constructor
function ProcessCodeTask() {
    this.inputFile = null;
    this.outputFile = null;
    this.verbose = false;
    this.beforeAllFilesHook = null;
    this.afterAllFilesHook = null;
    this.beforeFileHook = null;
    this.afterFileHook = null;
    this.allErrors = [];
}

ProcessCodeTask.prototype.logVerbose = function(msg) {
    if (this.verbose) {
        console.log("[VERBOSE] " + msg);
    }
};

ProcessCodeTask.prototype.logInfo = function(msg) {
    console.log("[INFO] " + msg);
};

ProcessCodeTask.prototype.logWarn = function(msg) {
    console.log("[WARN] " + msg);
};

ProcessCodeTask.prototype.execute = function(evalFunction, cb=null) {
    this.executeAsync(evalFunction).then(function() {
        cb & cb(null);
    }).catch(function(err) {
        cb && cb(err);
    });
}

ProcessCodeTask.prototype.executeAsync = async function(evalFunction) {
    // save properties before use.
    const inputFile = this.inputFile;
    const outputFile = this.outputFile;
    const beforeFileHook = this.beforeFileHook;
    const afterFileHook = this.afterFileHook;
    
    // validate
    assert.ok(inputFile, "inputFile property is not set");
    assert.ok(outputFile, "outputFile property is not set");
    assert.ok(evalFunction);

    const allErrors = [];
        
    // ensure dir exists for outputFile.
    const outputDir = path.dirname(outputFile)
    if (outputDir) {
        fs.mkdirSync(outputDir, {
            recursive: true // also ensures no error is thrown if dir already exists
        });
    }

    const context = new ProcessCodeContext();
    context.errorAccumulator = (message, e) => {
        allErrors.push(createOrWrapException(context, message, e));
    };

    let codeGenRequest;
    let codeGenResponse;
    try {
        codeGenRequest = fs.createReadStream(inputFile);
        codeGenResponse = fs.createWriteStream(outputFile);

        // begin serialize by writing header to output 
        await writeStreamAsync(codeGenResponse, JSON.stringify({}, '') + endOfLine);

        let headerSeen = false;
        for await (const line of wrapInputStreamWithReadLines(codeGenRequest)) {
            // begin deserialize by reading header from input
            if (!headerSeen) {
                context.header = JSON.parse(line);
                headerSeen = true;

                prepareContextForHookCall(context, true, null, null, null, -1);
                await callHook1(this.beforeAllFilesHook, context);
                continue;
            }
            
            const fileAugCodes = JSON.parse(line);
            const srcFile = path.join(fileAugCodes.dir,
                fileAugCodes.relativePath);
            this.logVerbose(`Processing ${srcFile}`);
            startInstant = new Date();

            const beginErrorCount  = allErrors.length;
            let fileGenCodes;
            try {
                fileGenCodes = await processFileOfAugCodes(context, evalFunction,
                    beforeFileHook, afterFileHook, srcFile,
                    fileAugCodes, allErrors)
            }
            catch (e) {
                allErrors.push(createOrWrapException(context, "processing error", e));
            }
            
            if (allErrors.length > beginErrorCount) {
                this.logWarn((allErrors.length - beginErrorCount) + " error(s) encountered in " + srcFile);
            }

            // don't waste time serializing if there are errors from previous
            // iterations or this current one.
            if (!allErrors.length) {
                await writeStreamAsync(codeGenResponse, JSON.stringify(fileGenCodes, '') + endOfLine);
            }

            endInstant = new Date();
            timeElapsed = endInstant.getTime() - startInstant.getTime();
            this.logInfo(`Done processing ${srcFile} in ${timeElapsed} ms`);            
                this.logInfo(`Done processing ${srcFile} in ${timeElapsed} ms`);            
            this.logInfo(`Done processing ${srcFile} in ${timeElapsed} ms`);            
        }
        prepareContextForHookCall(context, false, null, null, null, -1);
        await callHook1(this.afterAllFilesHook, context);
    }
    finally {
        this.allErrors = allErrors;
        await endStreamAsync(codeGenResponse);
    }
};

function wrapInputStreamWithReadLines(input) {
    const output = new stream.PassThrough({ objectMode: true });
    const rl = readline.createInterface({ input: input, crlfDelay: Infinity });
    rl.on("line", line => {
        output.write(line);
    });
    rl.on("close", () => {
        output.push(null);
    });
    return output;
}

function writeStreamAsync(streamInstance, data) {
    return new Promise((resolve, reject) => {
        streamInstance.write(data, function(err) {
            if (err) {
                reject(err);
            } else {
                resolve();
            }
        });
    })
}

function endStreamAsync(streamInstance) {
    if (streamInstance) {
        return new Promise((resolve, reject) => {
            streamInstance.end(function(err) {
                if (err) {
                    reject(err);
                } else {
                    resolve();
                }
            })
        });
    }
}

function callHook1(hook, context) {
    if (hook) {
        return new Promise((resolve, reject) => {
            hook(context, function(err) {
                if (err) {
                    reject(err);
                } else {
                    resolve();
                }
            });
        });
    }
}
    
function callHook2(hook, context) {
    if (hook) {
        return new Promise((resolve, reject) => {
            hook(context, function(err, res) {
                if (err) {
                    reject(err);
                } else {
                    resolve(res);
                }
            });
        });
    }
}

function prepareContextForHookCall(context,
        clearFileScope, srcFile, fileAugCodes, fileGenCodes, augCodeIndex) {
    if (clearFileScope) {
        context.fileScope = {};
    }
    context.srcFile = srcFile;
    context.fileAugCodes = fileAugCodes;
    context.fileGenCodes = fileGenCodes;
    context.augCodeIndex = augCodeIndex;
}

async function processFileOfAugCodes(context, evalFunction,
        beforeFileHook, afterFileHook, srcFile, fileAugCodes, errors) {
    let fileGenCodes;
    try {
        prepareContextForHookCall(context, true, srcFile,
            fileAugCodes, null, -1);
        fileGenCodes = await callHook2(beforeFileHook, context);
    }
    catch(e) {
        errors.push(createOrWrapException(context,
            "beforeFileHook error", e));
        return;
    }
    if (fileGenCodes === null || fileGenCodes === undefined) {
        // fetch arguments, and parse any json argument found.
        for (augCode of fileAugCodes.augmentingCodes) {
            augCode.processed = false;
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
        
        // now begin aug code processing.
        fileGenCodes = {
            fileId: 0, // declare here so as to provide deterministic position during tests
            generatedCodes: []
        };
        for (let i = 0; i < fileAugCodes.augmentingCodes.length; i++) {
            const augCode = fileAugCodes.augmentingCodes[i];
            if (augCode.processed) {
                continue;
            }

            const functionName = retrieveFunctionName(augCode);
            prepareContextForHookCall(context, false, srcFile, fileAugCodes,
                fileGenCodes, i);
            const genCodes = await processAugCode(evalFunction, functionName,
                augCode, context, errors);
            for (genCode of genCodes) {
                fileGenCodes.generatedCodes.push(genCode);
            }
        }
    }
    
    try {
        fileGenCodes.fileId = fileAugCodes.fileId;
        validateGeneratedCodeIds(fileGenCodes.generatedCodes, context, errors);
    }
    catch (e) {
        errors.push(createOrWrapException(context, "validation error", e));
        return;
    }
    
    try {
        prepareContextForHookCall(context, false, srcFile, fileAugCodes, fileGenCodes, -1);
        await callHook1(afterFileHook, context);
    }
    catch (e) {
        errors.push(createOrWrapException(context,
            "afterFileHook error", e));
        return;
    }
    return fileGenCodes;
}

function retrieveFunctionName(augCode) {
    let functionName = augCode.blocks[0].content.trim();
    return functionName;
}

async function processAugCode(evalFunction, functionName, augCode, context, errors) {
    try {
        let result = await evalFunction(functionName, augCode, context);
        if (result === null || typeof result === 'undefined') {
            return [ convertGenCodeItem(null) ];
        }
        let converted = [];
        if (Array.isArray(result)) {
            for (item of result) {
                let genCode = convertGenCodeItem(item);
                converted.push(genCode);
                // try and mark corresponding aug code as processed.
                if (genCode.id > 0) {
                    let correspondingAugCodes = 
                        context.fileAugCodes.augmentingCodes
                            .filter(x => x.id == genCode.id);
                    if (correspondingAugCodes.length > 0) {
                        correspondingAugCodes[0].processed = true;
                    }
                }
            }
        }
        else {
            let genCode = convertGenCodeItem(result);
            genCode.id = augCode.id;
            converted.push(genCode);
        }
        return converted;
    }
    catch (excObj) {
        errors.push(createOrWrapException(context, "", excObj));
        return [];
    }
}

function convertGenCodeItem(item) {
    if (item === null || typeof item === 'undefined') {
        return { id: 0 };
    }
    if (typeof item.skipped !== 'undefined' || typeof item.contentParts !== 'undefined') {
        // assume it is GeneratedCode instance and ensure
        // existence of id field.
        if (!item.id) {
            item.id = 0;
        }
        return item;
    }
    else if (typeof item.content !== 'undefined') {
        // assume it is ContentPart instance
        return {
            id: 0,
            contentParts: [ item ]
        };
    }
    else {
        // assume string or stringify it.
        return {
            id: 0,
            contentParts: [
                {
                    content: `${item}`,
                    exactMatch: false
                }
            ]
        };
    }
}

function validateGeneratedCodeIds(fileGenCodeList, context, errors) {
    if (!fileGenCodeList) {
        return;
    }
    let ids = fileGenCodeList.map(x => x.id);
    // Interpret use of -1 or negatives as intentional and skip
    // validating negative ids.
    if (ids.filter(x => !x).length > 0) {
        errors.push(createOrWrapException(context, 'At least one generated code id was not set. Found: ' + ids,
            null));
    }
}

function createOrWrapException(context, message, cause) {
    let wrapperMessage = '';
    let srcFileSnippet = null;
    try {
        if (context.srcFile) {
            wrapperMessage += `in ${context.srcFile}`;
            if (context.fileAugCodes.augmentingCodes) {
                if (context.augCodeIndex >= 0 && context.augCodeIndex < context.fileAugCodes.augmentingCodes.length) {
                    let augCode = context.fileAugCodes.augmentingCodes[context.augCodeIndex];
                    wrapperMessage += ` at line ${augCode.lineNumber}`;
                    srcFileSnippet = augCode.blocks[0].content;
                }    
            }
        }
    }
    catch (ignore) {}
    if (wrapperMessage) {
        wrapperMessage += ": ";
    }
    if (message !== null && message !== undefined) {
        wrapperMessage += message;
    }
    if (srcFileSnippet) {
        wrapperMessage += `\n\n${srcFileSnippet}`;
    }
    const wrapperException = new Error(wrapperMessage);
    if (cause) {
        wrapperException.cause = cause;
    }
    return wrapperException;
}

ProcessCodeTask.prototype.generateStackTrace = function(error) {
    return util.inspect(error);
}

module.exports = {
    ProcessCodeTask,
    ProcessCodeContext,
    CodeAugmentorFunctions: require('./CodeAugmentorFunctions')
};