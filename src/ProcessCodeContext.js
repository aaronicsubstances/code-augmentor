// Class constructor.
function ProcessCodeContext() {
    this.header = null;
    this.globalScope = {
        'code_indent': '    '
    };
    this.fileScope = {};
    this.fileAugCodes = null;
    this.fileGenCodes = null;
    this.augCodeIndex = 0;
    this.srcFile = null;
    this.errorAccumulator = null;
}

ProcessCodeContext.prototype.addError = function(message, cause) {
    this.errorAccumulator(message, cause);
};

ProcessCodeContext.prototype.newGenCode = function() {
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

ProcessCodeContext.prototype.newSkipGenCode = function(augCodeId) {
    if (this.fileGenCodes.augCodeIdsToSkip === null ||
        this.fileGenCodes.augCodeIdsToSkip === undefined) {
        this.fileGenCodes.augCodeIdsToSkip = [];
    }
    this.fileGenCodes.augCodeIdsToSkip.push(augCodeId);
    return [];
}

ProcessCodeContext.prototype.getScopeVar = function(name) {
    if (name in this.fileScope) {
        return this.fileScope[name];
    }
    return this.globalScope[name];
};

module.exports = ProcessCodeContext;