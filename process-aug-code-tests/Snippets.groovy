public class Snippets {
    static generateSerialVersionUID(augCode, context) {
        return "private static final int serialVersionUID = 23L;";
    }

    static stringify(augCode, context) {
        def g = context.newGenCode()
        for (int i = 0; i < augCode.args.size(); i++) {
            def s = '"' + augCode.args[i];
            if (i < augCode.args.size() - 1) {
                s += augCode.lineSeparator + '" +';
            }
            else {
                s += '"';
            }
            g.contentParts.add(context.newContent(s, true));
        }
        return g;
    }

    static generateGetter(augCode, context) {
        def name = augCode.args[0].name;
        def type = augCode.args[0].type;
        def nameCapitalized = '' + name.charAt(0).toUpperCase() + name.substring(1);
        def s = "public ${type} get${nameCapitalized}() {${augCode.lineSeparator}";
        s += "    return ${name};${augCode.lineSeparator}";
        s += '}';
        return context.newContent(s);
    }

    static intentionalBlock(augCode, context) {
        def endingAugCodeIndex = context.fileAugCodes.augmentingCodes.findIndexOf(
                context.augCodeIndex + 1) {
            return it.nestedLevelNumber == augCode.nestedLevelNumber &&
                it.hasNestedLevelEndMarker;
        };
        assert endingAugCodeIndex != -1;
        def endingAugCode = context.fileAugCodes.augmentingCodes[endingAugCodeIndex];
        def startGenCode = context.newGenCode();
        startGenCode.id = augCode.id;
        startGenCode.indent = "  ";
        startGenCode.contentParts.add(context.newContent("{"));
        def endGenCode = context.newGenCode();
        endGenCode.id = endingAugCode.id;
        endGenCode.indent = "  ";
        endGenCode.contentParts.add(context.newContent("}"));
        return [ startGenCode, endGenCode ];
    }

    static testSettingAllGenCodeProps(augCode, context) {
        def genCode = context.newGenCode();
        genCode.id = augCode.id;
        genCode.indent = "  ";
        genCode.replaceAugCodeDirectives = true;
        genCode.replaceGenCodeDirectives = true;
        genCode.disableAutoIndent = true;
        genCode.contentParts.push(context.newContent("//TODO", true));

        def nextAugCode = context.fileAugCodes.augmentingCodes[context.augCodeIndex + 1];
        def nextGenCode = context.newGenCode();
        nextGenCode.id = nextAugCode.id;
        nextGenCode.skipped = true;
        nextGenCode.contentParts.push(context.newContent(""));
        return [ genCode, nextGenCode ];
    }
}