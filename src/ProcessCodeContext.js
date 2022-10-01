// Class constructor.
function ProcessCodeContext(errorAccumulator) {
    this.header = null;
    this.globalScope = {
        'code_indent': '    '
    };
    this.fileScope = {};
    this.fileAugCodes = null;
    this.augCodeIndex = 0;
    this.srcFile = null;
    this.errorAccumulator = errorAccumulator;
}

ProcessCodeContext.prototype.addError = function(message, cause) {
    this.errorAccumulator(message, cause);
};

ProcessCodeContext.prototype.newGenCode = function(errorAccumulator) {
    return {
        id: 0,
        contentParts: []
    }
};

ProcessCodeContext.prototype.newContent = function(content, exactMatch=false) {
    return {
        content, exactMatch
    };
};

ProcessCodeContext.prototype.newSkipGenCode = function() {
    return {
        skipped: true
    };
}

ProcessCodeContext.prototype.getScopeVar = function(name) {
    if (name in this.fileScope) {
        return this.fileScope[name];
    }
    return this.globalScope[name];
};

module.exports = ProcessCodeContext;