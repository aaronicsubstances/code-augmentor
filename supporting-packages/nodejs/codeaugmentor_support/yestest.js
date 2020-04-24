exports.println = function(augCode, context) {
    return `println() called with ${JSON.stringify(augCode.args)}`;
}

exports.ifal = function(augCode, context) {
    return [{ content: `ifal() called with ${JSON.stringify(augCode.args)}` }];
}

exports.print = function(augCode, context) {
    return `print() called with ${JSON.stringify(augCode.args)}`;
}