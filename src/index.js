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
    // validate
    assert.ok(this.inputFile, "inputFile property is not set");
    assert.ok(this.outputFile, "outputFile property is not set");
    assert.ok(evalFunction);

    this.allErrors = [];
        
    // ensure dir exists for outputFile.
    const outputDir = path.dirname(this.outputFile)
    if (outputDir) {
        fs.mkdirSync(outputDir, {
            recursive: true // also ensures no error is thrown if dir already exists
        });
    }

    const context = new ProcessCodeContext();

    let codeGenRequest;
    let codeGenResponse;
    try {
        codeGenRequest = fs.createReadStream(this.inputFile);
        codeGenResponse = fs.createWriteStream(this.outputFile);

        // begin serialize by writing header to output 
        await writeStreamAsync(codeGenResponse, JSON.stringify({}, '') + endOfLine);

        let headerSeen = false;
        for await (const line of wrapInputStreamWithReadLines(codeGenRequest)) {
            // begin deserialize by reading header from input
            if (!headerSeen) {
                context.header = JSON.parse(line);
                headerSeen = true;

                await callBeforeAllFiles(this.beforeAllFilesHook, context); 
                continue;
            }
            
            let fileAugCodes = JSON.parse(line);

            // set up context.
            context.srcFile = path.join(fileAugCodes.dir,
                fileAugCodes.relativePath);
            context.fileAugCodes = fileAugCodes;
            context.fileScope = {};
            this.logVerbose(`Processing ${context.srcFile}`);
            startInstant = new Date();

            let fileErrors = [];
            context.augCodeIndex = -1;
            let fileGenCodes;
            try {
                fileGenCodes = await callBeforeFile(this.beforeFileHook, context);
            }
            catch (e) {
                fileErrors.push(createOrWrapException(context, "beforeFileHook error", e));
            }

            if (!fileGenCodes && !fileErrors.length) { 
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
                    fileId: fileAugCodes.fileId,
                    generatedCodes: []
                };
                for (let i = 0; i < fileAugCodes.augmentingCodes.length; i++) {
                    const augCode = fileAugCodes.augmentingCodes[i];
                    if (augCode.processed) {
                        continue;
                    }

                    context.augCodeIndex = i;
                    const functionName = retrieveFunctionName(augCode);
                    const genCodes = processAugCode(evalFunction, functionName,
                        augCode, context, fileErrors);
                    for (genCode of genCodes) {
                        fileGenCodes.generatedCodes.push(genCode);
                    }
                }
            }

            validateGeneratedCodeIds(fileGenCodes.generatedCodes, context, fileErrors);
            
            if (fileErrors.length) {
                this.logWarn(fileErrors.length + " error(s) encountered in " + context.srcFile);
            } 

            if (!this.allErrors.length && !fileErrors.length) {
                await writeStreamAsync(codeGenResponse, JSON.stringify(fileGenCodes, '') + endOfLine);
            }            
            for (e of fileErrors) {
                this.allErrors.push(e);
            }

            context.augCodeIndex = fileAugCodes.augmentingCodes.length;
            try {
                await callAfterFile(this.afterFileHook, context, fileErrors);
            }
            catch (e) {
                this.allErrors.push(createOrWrapException(context, "afterFileHook error", e));
            } 

            endInstant = new Date();
            timeElapsed = endInstant.getTime() - startInstant.getTime();
            this.logInfo(`Done processing ${context.srcFile} in ${timeElapsed} ms`);
        }

        context.srcFile = null;
        context.fileAugCodes = null;
        context.fileScope = {};
        await callAfterAllFiles(this.afterAllFilesHook, context);
    }
    finally {
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

function callBeforeAllFiles(hook, context) {
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
    
function callAfterAllFiles(hook, context) {
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
    
function callBeforeFile(hook, context) {
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

function callAfterFile(hook, context, fileErrors) {
    if (hook) {
        return new Promise((resolve, reject) => {
            hook(context, fileErrors, function(err) {
                if (err) {
                    reject(err);
                } else {
                    resolve();
                }
            });
        });
    }
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

function retrieveFunctionName(augCode) {
    let functionName = augCode.blocks[0].content.trim();
    return functionName;
}

function processAugCode(evalFunction, functionName, augCode, context, errors) {
    try {
        let result = evalFunction(functionName, augCode, context);
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
    else {
        let duplicateIds = ids.filter(x => x > 0 && ids.filter(y => x == y).length > 1);
        if (duplicateIds.length > 0) {
            errors.push(createOrWrapException(context, 'Valid generated code ids must be unique, but found duplicates: ' + ids,
                null));
        }
    }
}

function createOrWrapException(context, message, cause) {
    let wrapperMessage = '';
    let srcFileSnippet = null;
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
    if (wrapperMessage) {
        wrapperMessage += ": ";
    }
    wrapperMessage += message;
    if (srcFileSnippet) {
        wrapperMessage += `\n\n${srcFileSnippet}`;
    }
    const wrapperException = new Error(wrapperMessage, {
        cause: cause
    });
    if (wrapperException.cause !== cause) {
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