def generateSerialVersionUID(augCode, context):
    return "private static final int serialVersionUID = 23L;"

def stringify(augCode, context):
    g = context.newGenCode()
    for i in range(len(augCode.args)):
        s = '"' + augCode.args[i]
        if i < len(augCode.args) - 1:
            s += augCode.lineSeparator + '" +'
        else:
            s += '"'
        g.contentParts.append(context.newContent(s, True))
    return g

def generateGetter(augCode, context):
    name = augCode.args[0].name
    getterType = augCode.args[0].type
    nameCapitalized = name.capitalize()
    lineSep = augCode.lineSeparator
    s = f'public {getterType} get{nameCapitalized}() {{{lineSep}'
    s += f'    return {name};{lineSep}'
    s += '}'
    return context.newContent(s)

def intentionalBlock(augCode, context):
    endingAugCode = None
    for i in range(context.augCodeIndex + 1, len(context.fileAugCodes.augmentingCodes)):
        v = context.fileAugCodes.augmentingCodes[i]
        if v.nestedLevelNumber == augCode.nestedLevelNumber and \
                v.hasNestedLevelEndMarker:
            endingAugCode = v
            break
    assert endingAugCode
    startGenCode = context.newGenCode()
    startGenCode.id = augCode.id
    startGenCode.indent = "  "
    startGenCode.contentParts.append(context.newContent("{"))
    endGenCode = context.newGenCode()
    endGenCode.id = endingAugCode.id
    endGenCode.indent = "  "
    endGenCode.contentParts.append(context.newContent("}"))
    return [ startGenCode, endGenCode ]

def testSettingAllGenCodeProps(augCode, context):
    genCode = context.newGenCode()
    genCode.id = augCode.id
    genCode.replaceAugCodeDirectives = True
    genCode.replaceGenCodeDirectives = True
    genCode.disableEnsureEndingNewline = True
    genCode.contentParts.append(context.newContent("//TODO", True))

    nextAugCode = context.fileAugCodes.augmentingCodes[context.augCodeIndex + 1]
    nextGenCode = context.newGenCode()
    nextGenCode.id = nextAugCode.id
    nextGenCode.skipped = True
    nextGenCode.contentParts.append(context.newContent(""))
    return [ genCode, nextGenCode ]

def testNegativeIdBypass(augCode, context):
    g = context.newGenCode()
    g.id = -1
    g.contentParts.append(context.newContent('test'))
    return [ g ]

def testHeader(augCode, context):
    assert '//:GS:' == context.header.genCodeStartDirective
    assert '//:GE:' == context.header.genCodeEndDirective
    assert '//:STR:' == context.header.embeddedStringDirective
    assert '//:JSON:' == context.header.embeddedJsonDirective
    assert '//:SS:' == context.header.skipCodeStartDirective
    assert '//:SE:' == context.header.skipCodeEndDirective
    assert '//:AUG_CODE:' == context.header.augCodeDirective
    assert '//:GG:' == context.header.inlineGenCodeDirective
    assert '[' == context.header.nestedLevelStartMarker
    assert ']' == context.header.nestedLevelEndMarker
    return ''
