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
    assert.ok(augCode.contentWithinNestedMarkers == "intentionalContent");
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
    genCode.replaceAugCodeDirectives = true;
    genCode.replaceGenCodeDirectives = true;
    genCode.disableEnsureEndingNewline = true;
    genCode.contentParts.push(context.newContent("//TODO", true));

    const nextAugCode = context.fileAugCodes.augmentingCodes[context.augCodeIndex + 1];
    assert.ok(nextAugCode);    
    const nextGenCode = context.newGenCode();
    nextGenCode.id = nextAugCode.id;
    nextGenCode.skipped = true;
    nextGenCode.contentParts.push(context.newContent(""));
    return [ genCode, nextGenCode ];
}

exports.testNegativeIdBypass = function(augCode, context) {
    const g = context.newGenCode();
    g.id = -1;
    g.contentParts.push(context.newContent('test'));
    return [ g ];
}

exports.testHeader = function(augCode, context) {
    assert.equal('//:GS:', context.header.genCodeStartDirective);
    assert.equal('//:GE:', context.header.genCodeEndDirective);
    assert.equal('//:STR:', context.header.embeddedStringDirective);
    assert.equal('//:JSON:', context.header.embeddedJsonDirective);
    assert.equal('//:SS:', context.header.skipCodeStartDirective);
    assert.equal('//:SE:', context.header.skipCodeEndDirective);
    assert.equal('//:AUG_CODE:', context.header.augCodeDirective);
    assert.equal('//:GG:', context.header.inlineGenCodeDirective);
    assert.equal('[', context.header.nestedLevelStartMarker);
    assert.equal(']', context.header.nestedLevelEndMarker);
    return '';
}