const path = require('path');

const OtherFunctions = require('./OtherFunctions');

exports.theClassProps = function(augCode, context) {
    context.fileScope.genCodes = (function*() {
        //const defaultIndent = context.globalScope['code_indent'];
        const augCodeNode = augCode.leadNode;
        const indent = augCodeNode.indent;
        const lineSeparator = augCodeNode.lineSep;
    
        context.fileScope.theClassProps = JSON.parse(augCode.data[1])
        context.fileScope.theClassName = path.basename(context.fileScope.srcPath, '.java')
        let out = ''
        for (propSpec of context.fileScope.theClassProps) {
            out += `private ${propSpec.type} ${propSpec.name};`
            out += lineSeparator
        }
        const g = {
            contentParts: [],
            indent
        }
        g.contentParts.push({ content: out })
        yield g
    })()
}

exports.generateClassProps = function(augCode, context) {
    const defaultIndent = context.globalScope['code_indent'];
    const augCodeNode = augCode.leadNode;
    const indent = augCodeNode.indent;
    const lineSeparator = augCodeNode.lineSep;

    let out = ''
    for (propSpec of context.fileScope.theClassProps) {
        const capitalized = OtherFunctions.capitalize(propSpec.name);
        out += `public ${propSpec.type} get${capitalized}() {`
        out += lineSeparator
        out += `${defaultIndent}return ${propSpec.name};`
        out += lineSeparator
        out += `}${lineSeparator}`
        out += `public void set${capitalized}(${propSpec.type} ${propSpec.name}) {`
        out += lineSeparator
        out += `${defaultIndent}this.${propSpec.name} = ${propSpec.name};`
        out += lineSeparator
        out += `}${lineSeparator}`
        out += lineSeparator
    }
    const g = {
        contentParts: [],
        indent: indent
    };
    g.contentParts.push({ content: out })
    context.fileScope.genCodes = g
}

exports.generateEqualsAndHashCode = function(augCode, context) {
    // don't override if empty.
    if (context.fileScope.theClassProps.length == 0) {
        return;
    }

    const defaultIndent = context.globalScope['code_indent'];
    const augCodeNode = augCode.leadNode;
    const indent = augCodeNode.indent;
    const lineSeparator = augCodeNode.lineSep;
    
    let out = '';

    // generate equals() override
    out += `@Override${lineSeparator}`
    out += `public boolean equals(Object obj) {`
    out += lineSeparator
    out += `${defaultIndent}if (!(obj instanceof ${context.fileScope.theClassName})) {`
    out += lineSeparator
    out += `${defaultIndent}${defaultIndent}return false;`
    out += lineSeparator
    out += `${defaultIndent}` + '}'
    out += lineSeparator
    out += `${defaultIndent}${context.fileScope.theClassName} other = (${context.fileScope.theClassName}) obj;`
    out += lineSeparator
    
    for (propSpec of context.fileScope.theClassProps) {
        if (OtherFunctions.isUpperCase(propSpec.type[0])) {
            out += defaultIndent
            out += 'if (!Objects.equals(this.'
            out += propSpec.name;
            out += ', other.' 
            out += propSpec.name
            out += ')) {'
        }
        else {
            out += defaultIndent
            out += 'if (this.'
            out += propSpec.name;
            out += ' != other.' 
            out += propSpec.name
            out += ') {'
        }
        out += lineSeparator
        out += `${defaultIndent}${defaultIndent}return false;`
        out += lineSeparator
        out += defaultIndent + '}'
        out += lineSeparator
    }
    
    out += `${defaultIndent}return true;${lineSeparator}`
    out += '}'
    out += lineSeparator
    out += lineSeparator
    
    // generate hashCode() override with Objects.hashCode()
    out += `@Override${lineSeparator}`
    out += `public int hashCode() {`
    out += lineSeparator
    if (context.fileScope.theClassProps.length == 1) {
        out += `${defaultIndent}return Objects.hashCode(`
        out += context.fileScope.theClassProps[0].name
    }
    else {
        out += `${defaultIndent}return Objects.hash(`
        for (let i = 0; i < context.fileScope.theClassProps.length; i++) {
            if (i > 0) {
                out += ', '
            }
            out += context.fileScope.theClassProps[i].name
        }
    }
    out += `);${lineSeparator}`
    out += '}'
    out += lineSeparator
    const g = {
        contentParts: [],
        indent
    };
    g.contentParts.push({ content: out })
    context.fileScope.genCodes.push(g)
}

exports.generateToString = function(augCode, context) {
    const defaultIndent = context.globalScope['code_indent'];
    const augCodeNode = augCode.leadNode;
    const indent = augCodeNode.indent;
    const lineSeparator = augCodeNode.lineSep;

    let out = '';
    out += `@Override${lineSeparator}`
    out += `public String toString() {`
    out += lineSeparator
    out += `${defaultIndent}return String.format(getClass().getSimpleName() + `
    let exactOut = `"{`;
    let outArgs = '';
    for (let i = 0; i < context.fileScope.theClassProps.length; i++) {
        if (i > 0) {
            exactOut += ', '
            outArgs += ', '
        }
        exactOut += context.fileScope.theClassProps[i].name + '=%s'
        outArgs += context.fileScope.theClassProps[i].name
    }
    exactOut += '}"'
    const g = {
        contentParts: [],
        indent: indent
    };
    g.contentParts.push({ content: out });
    g.contentParts.push({ content: exactOut, exempt: true });
    out = '' // reset
    if (outArgs) {
        out += ",";
        out += lineSeparator;
        out += defaultIndent;
        out += defaultIndent;
    }
    out += outArgs
    out += `);${lineSeparator}`
    out += '}'
    out += lineSeparator
    g.contentParts.push({ content: out })
    context.fileScope.genCodes.push(g)
}
