const path = require('path')

const OtherFunctions = require('./OtherFunctions')

exports.theClassProps = function(augCode, context) {
    context.fileScope.genCodes = (function*() {
        //const defaultIndent = context.globalScope['code_indent'];
        //const augCodeNode = augCode.leadNode;
        //const indent = augCodeNode.indent;
        //const lineSeparator = augCodeNode.lineSep;
    
        context.fileScope.theClassProps = JSON.parse(augCode.data[1])
        context.fileScope.theClassName = path.basename(context.fileScope.srcPath, '.java')
        for (propSpec of context.fileScope.theClassProps) {
            yield `private ${propSpec.type} ${propSpec.name};\n`
        }
    })()
}

exports.generateClassProps = function(augCode, context) {
    context.fileScope.genCodes = (function*() {
        for (propSpec of context.fileScope.theClassProps) {
            const capitalized = OtherFunctions.capitalize(propSpec.name)
            yield `public ${propSpec.type} get${capitalized}() {\n`
            yield "" // apply indent
            yield `return ${propSpec.name};\n`
            yield '}\n'
            yield `public void set${capitalized}(${propSpec.type} ${propSpec.name}) {\n`
            yield "" // apply indent
            yield `this.${propSpec.name} = ${propSpec.name};\n`
            yield '}\n'
            yield "\n"
        }
    })()
}

exports.generateEqualsAndHashCode = function(augCode, context) {
    context.fileScope.genCodes = (function*() {
        // don't override if empty.
        if (context.fileScope.theClassProps.length == 0) {
            return;
        }

        // generate equals() override
        yield `@Override\n`
        yield `public boolean equals(Object obj) {\n`
        yield "" // apply indent
        yield `if (!(obj instanceof ${context.fileScope.theClassName})) {\n`
        yield "" // apply indent
        yield "" // apply indent
        yield `return false;\n`
        yield "" // apply indent
        yield '}\n'
        yield "" // apply indent
        yield `${context.fileScope.theClassName} other = (${context.fileScope.theClassName}) obj;\n`
        
        for (propSpec of context.fileScope.theClassProps) {
            if (OtherFunctions.isUpperCase(propSpec.type[0])) {
                yield "" // apply indent
                yield `if (!Objects.equals(this.${propSpec.name}, other.${propSpec.name})) {`
            }
            else {
                yield "" // apply indent
                yield `if (this.${propSpec.name} != other.${propSpec.name}) {`
            }
            yield "\n"
            yield "" // apply indent
            yield "" // apply indent
            yield 'return false;\n'
            yield "" // apply indent
            yield '}\n'
        }
        
        yield "" // apply indent
        yield 'return true;\n'
        yield '}\n\n'
        
        // generate hashCode() override with Objects.hashCode()
        yield `@Override\n`
        yield `public int hashCode() {\n`
        if (context.fileScope.theClassProps.length == 1) {
            yield "" // apply indent
            yield 'return Objects.hashCode('
            yield { exempt: true } // don't touch next item
            yield context.fileScope.theClassProps[0].name
        }
        else {
            yield "" // apply indent
            yield 'return Objects.hash('
            for (let i = 0; i < context.fileScope.theClassProps.length; i++) {
                if (i > 0) {
                    yield { exempt: true } // don't touch next item
                    yield ", "
                }
                yield { exempt: true } // don't touch next item
                yield context.fileScope.theClassProps[i].name
            }
        }
        yield { indent: '' } // don't indent next item
        yield ');\n'
        yield '}\n'
    })()
}

exports.generateToString = function(augCode, context) {
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
}
