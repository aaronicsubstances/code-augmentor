// Class constructor.
function ProcessCodeContext() {
    this.header = null;
    this.globalScope = new Map();
    this.fileScope = new Map();
    this.fileAugCodes = null;
    this.fileGenCodes = null;
    this.augCodeIndex = 0;
    this.srcFile = null;
    this.errorAccumulator = null;
    this.globalScope.set('code_indent', '    ');
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

ProcessCodeContext.prototype.getScopeVar = function(name) {
    if (this.fileScope.has(name)) {
        return this.fileScope.get(name);
    }
    return this.globalScope.get(name);
};

ProcessCodeContext.prototype.setScopeVar = function(name, value) {
    this.fileScope.set(name, value);
};

ProcessCodeContext.prototype.setGlobalScopeVar = function(name, value) {
    this.globalScope.set(name, value);
}

module.exports = ProcessCodeContext;