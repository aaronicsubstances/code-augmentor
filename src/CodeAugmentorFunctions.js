exports.setScopeVar = function(augCode, context) {
    modifyScope(context.fileScope, augCode);
    return context.newSkipGenCode(augCode.id);
};

exports.setGlobalScopeVar = function(augCode, context) {
    modifyScope(context.globalScope, augCode);
    return context.newSkipGenCode(augCode.id);
};

function modifyScope(scope, augCode) {
    for (arg of augCode.args) {
        Object.assign(scope, arg);
    }
}