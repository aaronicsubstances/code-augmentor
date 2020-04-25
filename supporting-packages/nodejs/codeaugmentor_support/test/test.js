const assert = require('assert').strict;
const fs = require('fs');
const path = require('path');

const rimraf = require('rimraf');

const codeaugmentor_support = require('../index.js');

describe('Array', function() {
  describe('#indexOf()', function() {
    it('should return -1 when the value is not present', function() {
      assert.equal([1, 2, 14, 3].indexOf(4), -1);
    });
  });
});

describe('codeaugmentor_support', function() {
    it('should execute basic usage successfully', function(done) {
        const tmpdir = path.join(path.dirname(__dirname), 'build');
        // intentionally ignore whether or not tmpdir exists
        const config = {
            inputFile: path.join(__dirname, 'basic_usage_aug_codes.json'),
            outputFile: path.join(tmpdir, 'basic_usage_gen_codes.json'),
            verbose: true
        };
        
        codeaugmentor_support.execute(config, evaler, function(err) {
            done(err);
            assert.ok(!config.allErrors.length);
            console.log('Output successfully written to ' + config.outputFile);
        });
    });
});

describe('codeaugmentor_support', function() {
    it('should execute basic eval exception successfully', function(done) {
        const tmpdir = path.join(path.dirname(__dirname), 'build');
        // intentionally delete tmpdir
        rimraf.sync(tmpdir);
        const config = {
            inputFile: path.join(__dirname, 'basic_usage_aug_codes.json'),
            outputFile: path.join(tmpdir, 'basic_usage_gen_codes.json'),
            verbose: true
        };
        
        codeaugmentor_support.execute(config, productionEvaler, function(err) {
            done(err);
            assert.ok(config.allErrors.length);
            console.log('Expected errors, and found ' + config.allErrors.length);
            for (ex of config.allErrors) {
                console.log(ex);
                //console.error(ex);
            }
        });
    });
});

function evaler(functionName, augCode, context) {
    return `Received: ${functionName}: ${augCode}, ${context}`;
}

function productionEvaler(functionName, augCode, context) {
    return eval(functionName + '(augCode, context)');
}