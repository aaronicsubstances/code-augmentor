const assert = require('assert').strict;
const fs = require('fs');
const path = require('path');

const rimraf = require('rimraf');
const tempDirectory = require('temp-dir');

const code_augmentor_support = require('../src/index');

// pre-import for use by scripts in testing of scope accesses...
const CodeAugmentorFunctions = require('../src/CodeAugmentorFunctions');

let buildDir = path.join(tempDirectory, "code-augmentor-support-nodejs");

/**
 * Main purpose of tests in this project is to test
 * error cases and the formatting of thrown exceptions.
 * More thorough testing of success case scenerios is dealt with outside this
 * project.
 */

describe('code_augmentor_support', function() {
    it('should execute basic usage successfully', function(done) {
        // test that output dir can be recreated if absent.
        // do this only here, so subsequent tests verify that
        // existing output dir can be used successfully.
        rimraf.sync(buildDir);
        const task = new code_augmentor_support.ProcessCodeTask();

        // test property accessors.
        assert.equal(null, task.inputFile);
        assert.equal(null, task.outputFile);
        assert.equal(false, task.verbose);
        assert.equal(null, task.beforeAllFilesHook);
        assert.equal(null, task.afterAllFilesHook);
        assert.equal(null, task.beforeFileHook);
        assert.equal(null, task.afterFileHook);
        assert.deepEqual(task.allErrors, []);

        const hookLogs = [];
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-00.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-00.json');
        task.verbose = true;
        task.beforeAllFilesHook = (context, cb) => {
            hookLogs.push("beforeAllFiles");
            cb();
        };
        task.afterAllFilesHook = (context, cb) => {
            hookLogs.push("afterAllFiles");
            cb();
        };
        task.beforeFileHook = (context, cb) => {
            hookLogs.push("beforeFile");
            cb();
        };
        task.afterFileHook = (context, cb) => {
            hookLogs.push("afterFile");
            cb();
        };

        // test property accessors again.
        assert.ok(task.inputFile);
        assert.ok(task.outputFile);
        assert.equal(true, task.verbose);
        assert.ok(task.beforeAllFilesHook);
        assert.ok(task.afterAllFilesHook);
        assert.ok(task.beforeFileHook);
        assert.ok(task.afterFileHook);
        assert.deepEqual(task.allErrors, []);
        
        task.execute(evaler, function(err) {
            done(err);
            printErrors(task);
            assert.deepEqual(hookLogs, ["beforeAllFiles", "beforeFile",
                "afterFile", "beforeFile", "afterFile", "afterAllFiles"])
            assert.ok(!task.allErrors.length);
        });
    });
});

describe('code_augmentor_support', function() {
    it('should fail due to unset ids', function(done) {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-00.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.verbose = true;
        
        task.execute(evalerProducingUnsetIds, function(err) {
            done(err);
            printErrors(task);
            assert.equal(task.allErrors.length, 2);
            console.log(`Expected ${task.allErrors.length} error(s)`);
        });
    });
});

describe('code_augmentor_support', function() {
    it('should fail due to absence of production usage context', function(done) {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-01.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.verbose = true;
        
        task.execute(productionEvaler, function(err) {
            done(err);
            printErrors(task);
            assert.equal(task.allErrors.length, 2);
            console.log(`Expected ${task.allErrors.length} error(s)`);
        });
    });
});

describe('code_augmentor_support', function() {
    it('should fail due to missing evaler return value', function(done) {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-01.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.verbose = true;
        
        task.execute(function(f, a, c){}, function(err) {
            done(err);
            printErrors(task);
            assert.equal(task.allErrors.length, 1);
            console.log(`Expected ${task.allErrors.length} error(s)`);
        });
    });
});

describe('code_augmentor_support', function() {
    it('should pass testing of scope accesses and gen code skipping', function(done) {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-02.json');
        task.outputFile = path.join(buildDir, 'actual-genCodes-02.json');
        
        task.execute(contextScopeMethodAccessEvaler, function(err) {
            if (err) {
                done(err);
                return;
            }
            printErrors(task);
            assert.ok(!task.allErrors.length);
            fs.readFile(task.outputFile, 'utf8', function(err, data) {
                done(err)
                assert.equal(data.replace(/\r\n|\n|\r/g, "\n"), '{}\n' +
                    '{"fileId":1,"generatedCodes":[],' +
                    '"augCodeIdsToSkip":[1,2,3]}\n'
                );
            });
        });
    });
});

describe('code_augmentor_support', function() {
    it('should pass testing of typical before file usage', function(done) {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-02.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-03.json');
        task.beforeFileHook = function(context, cb) {
            cb(null, {
                skipped: true
            });
        };
        
        task.execute(shouldNotHaveRunEvaler, function(err) {
            if (err) {
                done(err);
                return;
            }
            printErrors(task);
            assert.ok(!task.allErrors.length);
            fs.readFile(task.outputFile, 'utf8', function(err, data) {
                done(err)
                assert.equal(data.replace(/\r\n|\n|\r/g, "\n"), '{}\n' +
                    '{"skipped":true,"fileId":1}\n'
                );
            });
        });
    });
});

describe('code_augmentor_support', function() {
    it('should pass testing of validation error resulting from before file usage', function(done) {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-02.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.beforeFileHook = function(context, cb) {
            const obj  = {};
            Object.defineProperty(obj, 'fileId', {
                get() {
                    return 0;
                },
                set(newValue) {
                    if (newValue) {
                        throw new Error("example setter error");
                    }
                }
            });
            cb(null, obj);
        };
        
        task.execute(shouldNotHaveRunEvaler, function(err) {
            done(err);
            printErrors(task);
            assert.equal(task.allErrors.length, 1);
            console.log(`Expected ${task.allErrors.length} error(s)`);
        });
    });
});

describe('code_augmentor_support', function() {
    it('should fail if before all files hook fails', async function() {
        const task = new code_augmentor_support.ProcessCodeTask();
        const hookLogs = [];
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-02.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.beforeAllFilesHook = function(context, cb) {
            hookLogs.push("beforeAllFiles");
            cb(new Error("from beforeAllFiles hook"));
        };
        task.afterAllFilesHook = function(context, cb) {
            hookLogs.push("afterAllFiles");
            cb();
        };
        await assert.rejects(async function() {
                    try {
                        await task.executeAsync(evaler);
                        printErrors(task);
                    }
                    catch (e) {
                        assert.deepEqual(hookLogs, ["beforeAllFiles"]);
                        throw e;
                    }
                },
                err => {
                    assert(err instanceof Error);
                    assert(/beforeAllFiles/.test(err));
                    return true;
                });
    });
});

describe('code_augmentor_support', function() {
    it('should fail if after all files hook fails', async function() {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-02.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.afterAllFilesHook = function(context, cb) {
            cb(new Error("from afterAllFiles hook"));
        };
        await assert.rejects(task.executeAsync(evaler)
                .then(() => printErrors(task)),
                err => {
                    assert(err instanceof Error);
                    assert(/afterAllFiles/.test(err));
                    return true;
                });
    });
});

describe('code_augmentor_support', function() {
    it('should pass testing of before file failure and context add error', async function() {
        const task = new code_augmentor_support.ProcessCodeTask();
        const hookLogs = [];
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-02.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.beforeFileHook = function(context, cb) {
            hookLogs.push("beforeFile");
            context.addError("test addError");
            cb(new Error("from beforeFile hook"));
        };
        task.afterAllFilesHook = function(context, cb) {
            hookLogs.push("afterAllFiles");
            cb();
        };
        task.afterFileHook = function(context, cb) {
            hookLogs.push("afterFile");
            cb();
        };
        
        await task.executeAsync(evaler);
        printErrors(task);
        assert.equal(task.allErrors.length, 2);
        assert.deepEqual(hookLogs, ["beforeFile", "afterAllFiles"]);
        console.log(`Expected ${task.allErrors.length} error(s)`);
    });
});

describe('code_augmentor_support', function() {
    it('should pass testing of after file failure', async function() {
        const task = new code_augmentor_support.ProcessCodeTask();
        task.inputFile = path.join(__dirname, 'resources', 'aug_codes-02.json');
        task.outputFile = path.join(buildDir, 'genCodes-js-ignore.json');
        task.afterFileHook = function(context, cb) {
            cb(new Error("from afterFile hook"));
        };
        
        await task.executeAsync(evaler);
        printErrors(task);
        assert.equal(task.allErrors.length, 1);
        console.log(`Expected ${task.allErrors.length} error(s)`);
    });
});

function printErrors(task) {
    for (error of task.allErrors) {
        console.log(task.generateStackTrace(error));
    }
}

function evaler(functionName, augCode, context) {
    return `Received: ${functionName}: ${augCode}, ${context}`;
}

function evalerProducingUnsetIds(functionName, augCode, context) {
    let genCode = context.newGenCode();
    //genCode.id = augCode.id;
    genCode.contentParts.push(context.newContent(`Received: ${functionName}`));
    return [ genCode ];
}

function productionEvaler(functionName, augCode, context) {
    return eval(functionName + '(augCode, context)');
}

function contextScopeMethodAccessEvaler(f, a, c) {
    if (f != "\"testUseOfGetScopeVar\"") {
        return productionEvaler(f, a, c);
    }
    assert.equal(c.getScopeVar("address"), "NewTown");
    assert.equal(c.getScopeVar("serviceType"), "ICT");
    assert.equal(c.getScopeVar("allServiceTypes"), "ICT,Agric");
    assert.equal(c.globalScope["address"], "OldTown");
    assert.equal(c.getScopeVar("code_indent"), "    ");
    return c.newSkipGenCode(a.id);
}

function shouldNotHaveRunEvaler(f, a, c) {
    throw new Error("should not have run");
}