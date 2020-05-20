const assert = require('assert').strict;

exports.generateSerialVersionUID = function(augCode, context) {
    return "private static final int serialVersionUID = 23L;";
}

exports.stringify = function(augCode, context) {
    const g = context.newGenCode()
    for (let i = 0; i < augCode.args.length; i++) {
        let s = '"' + augCode.args[i];
        if (i < augCode.args.length - 1) {
            s += augCode.lineSeparator + '" +';
        }
        else {
            s += '"';
        }
        g.contentParts.push(context.newContent(s, true));
    }
    return g;
}

exports.generateGetter = function(augCode, context) {
    const name = augCode.args[0].name;
    const type = augCode.args[0].type;
    const nameCapitalized = name.charAt(0).toUpperCase() + name.slice(1);
    let s = `public ${type} get${nameCapitalized}() {${augCode.lineSeparator}`;
    s += `    return ${name};${augCode.lineSeparator}`;
    s += '}';
    return context.newContent(s);
}

exports.intentionalBlock = function(augCode, context) {
    const augCodeWithEndMarkers = context.fileAugCodes.augmentingCodes.filter((v, i) => 
        i > context.augCodeIndex &&
        v.nestedLevelNumber == augCode.nestedLevelNumber &&
        v.hasNestedLevelEndMarker);
    assert.ok(augCodeWithEndMarkers.length);
    endingAugCode = augCodeWithEndMarkers[0];
    const startGenCode = context.newGenCode();
    startGenCode.id = augCode.id;
    startGenCode.indent = "  ";
    startGenCode.contentParts.push(context.newContent("{"));
    const endGenCode = context.newGenCode();
    endGenCode.id = endingAugCode.id;
    endGenCode.indent = "  ";
    endGenCode.contentParts.push(context.newContent("}"));
    return [ startGenCode, endGenCode ];
}

exports.testSettingAllGenCodeProps = function(augCode, context) {
    const genCode = context.newGenCode();
    genCode.id = augCode.id;
    genCode.indent = "  ";
    genCode.replaceAugCodeDirectives = true;
    genCode.replaceGenCodeDirectives = true;
    genCode.disableAutoIndent = true;
    genCode.contentParts.push(context.newContent("//TODO", true));

    const nextAugCode = context.fileAugCodes.augmentingCodes[context.augCodeIndex + 1];
    assert.ok(nextAugCode);    
    const nextGenCode = context.newGenCode();
    nextGenCode.id = nextAugCode.id;
    nextGenCode.skipped = true;
    nextGenCode.contentParts.push(context.newContent(""));
    return [ genCode, nextGenCode ];
}