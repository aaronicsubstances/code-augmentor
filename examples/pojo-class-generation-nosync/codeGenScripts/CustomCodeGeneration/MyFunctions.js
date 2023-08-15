const path = require('path')
const {
    AstParser,
    DefaultAstTransformer
} = require("code-augmentor-support")
const OtherFunctions = require('./OtherFunctions')

function removeAugCodeParts(augCode, context) {
    for (const p of context.getSourceCodeParts(augCode)) {
        if (p.type === DefaultAstTransformer.TYPE_AUG_CODE ||
                p.type === DefaultAstTransformer.TYPE_AUG_CODE_ARG) {
            p.updates = (p.updates || []).filter(v => v !== p.node)
        }
    }
}

exports.theClassProps = async function(augCode, context) {
    if (context.fileScope.generatedCodeInserted) {
        return removeAugCodeParts(augCode, context)
    }
    context.fileScope.genCodeList = (async function*() {
        //const defaultIndent = context.globalScope['code_indent'];
        //const augCodeNode = augCode.leadNode;
        //const indent = augCodeNode.indent;
        //const lineSeparator = augCodeNode.lineSep;
    
        context.fileScope.theClassProps = JSON.parse(augCode.data[1])
        context.fileScope.theClassName = path.basename(context.fileScope.srcPath, '.java')
        const out = []
        const g = {
            markerType: AstParser.TYPE_SOURCE_CODE,
            contentParts: out
        }
        for (propSpec of context.fileScope.theClassProps) {
            out.push(`private ${propSpec.type} ${propSpec.name};\n`)
        }
        yield g
    })()
    return true
}

exports.generateClassProps = function(augCode, context) {
    if (context.fileScope.generatedCodeInserted) {
        return removeAugCodeParts(augCode, context)
    }

    const out = []
    const g = {
        markerType: AstParser.TYPE_SOURCE_CODE,
        contentParts: out
    }
    for (propSpec of context.fileScope.theClassProps) {
        const capitalized = OtherFunctions.capitalize(propSpec.name)
        out.push(`public ${propSpec.type} get${capitalized}() {\n`)
        out.push("") // apply indent
        out.push(`return ${propSpec.name};\n`)
        out.push('}\n')
        out.push(`public void set${capitalized}(${propSpec.type} ${propSpec.name}) {\n`)
        out.push("") // apply indent
        out.push(`this.${propSpec.name} = ${propSpec.name};\n`)
        out.push('}\n')
        out.push("\n")
    }
    context.fileScope.genCodeList = g
    return true
}

exports.generateEqualsAndHashCode = function(augCode, context) {
    // don't override if empty.
    if (context.fileScope.theClassProps.length == 0) {
        return;
    }
    if (context.fileScope.generatedCodeInserted) {
        return removeAugCodeParts(augCode, context)
    }
    
    let out = [];

    // generate equals() override
    out.push(`@Override\n`)
    out.push(`public boolean equals(Object obj) {\n`)
    out.push("") // apply indent
    out.push(`if (!(obj instanceof ${context.fileScope.theClassName})) {\n`)
    out.push("") // apply indent
    out.push("") // apply indent
    out.push(`return false;\n`)
    out.push("") // apply indent
    out.push('}\n')
    out.push("") // apply indent
    out.push(`${context.fileScope.theClassName} other = (${context.fileScope.theClassName}) obj;\n`)
    
    for (propSpec of context.fileScope.theClassProps) {
        if (OtherFunctions.isUpperCase(propSpec.type[0])) {
            out.push("") // apply indent
            out.push(`if (!Objects.equals(this.${propSpec.name}, other.${propSpec.name})) {`)
        }
        else {
            out.push("") // apply indent
            out.push(`if (this.${propSpec.name} != other.${propSpec.name}) {`)
        }
        out.push("\n")
        out.push("") // apply indent
        out.push("") // apply indent
        out.push('return false;\n')
        out.push("") // apply indent
        out.push('}\n')
    }
    
    out.push("") // apply indent
    out.push('return true;\n')
    out.push('}\n\n')
    
    // generate hashCode() override with Objects.hashCode()
    out.push(`@Override\n`)
    out.push(`public int hashCode() {\n`)
    if (context.fileScope.theClassProps.length == 1) {
        out.push("") // apply indent
        out.push('return Objects.hashCode(')
        out.push({
            exempt: true,
            indent: '',
            lineSep: ''
        }) // don't touch next item
        out.push(context.fileScope.theClassProps[0].name)
    }
    else {
        out.push("") // apply indent
        out.push('return Objects.hash(')
        for (let i = 0; i < context.fileScope.theClassProps.length; i++) {
            if (i > 0) {
                out.push({ exempt: true }) // don't touch next item
                out.push(", ")
            }
            out.push({ exempt: true }) // don't touch next item
            out.push(context.fileScope.theClassProps[i].name)
        }
    }
    out.push({ indent: '' }) // don't indent next item
    out.push(');\n')
    out.push('}\n')
    const g = {
        markerType: AstParser.TYPE_SOURCE_CODE,
        contentParts: out
    };
    context.fileScope.genCodeList.push(g)
    return true
}

exports.generateToString = function(augCode, context) {
    if (context.fileScope.generatedCodeInserted) {
        return removeAugCodeParts(augCode, context)
    }

    let out = []
    out.push('@Override\n')
    out.push('public String toString() {\n')
    out.push("")
    out.push('return String.format(getClass().getSimpleName() + ')
    let exactOut = ['"{']
    let outArgs = []
    for (let i = 0; i < context.fileScope.theClassProps.length; i++) {
        if (i > 0) {
            exactOut.push(', ')
            outArgs.push({ indent: '' }) // don't indent next item
            outArgs.push(', ')
        }
        exactOut.push(context.fileScope.theClassProps[i].name)
        exactOut.push('=%s')
        outArgs.push({ indent: '' }) // don't indent next item
        outArgs.push(context.fileScope.theClassProps[i].name)
    }
    exactOut.push('}"')
    const g = {
        markerType: AstParser.TYPE_SOURCE_CODE,
        contentParts: []
    };
    g.contentParts.push(...out);
    g.contentParts.push({ exempt: true }) // don't touch next item
    g.contentParts.push(exactOut.join(""))
    out = [] // reset
    if (outArgs.length > 0) {
        out.push({ indent: '' }) // don't indent next item
        out.push(",\n")
        out.push("") // apply indent
        out.push("") // apply indent
        out.push("") // apply indent
    }
    out.push(...outArgs)
    out.push({ indent: '' }) // don't indent next item
    out.push(');\n')
    out.push('}\n')
    g.contentParts.push(...out)
    context.fileScope.genCodeList.push(g)
    context.fileScope.genCodeList.push(null)
    context.fileScope.genCodeList.push({
        ignore: true
    })
    return true
}
