class MyFunctions {
    
    static theClassProps(augCode, context) {
        context.fileScope.theClassProps = augCode.args[0]
        String fileWithoutExt = context.fileAugCodes.relativePath
        if (context.fileAugCodes.relativePath.contains('.')) {
            fileWithoutExt = context.fileAugCodes.relativePath.take(
                context.fileAugCodes.relativePath.lastIndexOf('.'))
        }
        context.fileScope.theClassName = fileWithoutExt
        def out = ''
        for (propSpec in context.fileScope.theClassProps) {
            out += "private ${propSpec.type} ${propSpec.name};"
            out += augCode.lineSeparator
        }
        final g = context.newGenCode()
        g.contentParts.add(context.newContent(out))
        g.replaceAugCodeDirectives = true
        g.indent = augCode.indent
        return g
    }

    static generateClassProps(augCode, context) {
        def out = ''
        for (propSpec in context.fileScope.theClassProps) {
            final capitalized = propSpec.name.capitalize();
            out += "public ${propSpec.type} get${capitalized}() {"
            out += augCode.lineSeparator
            out += "${OtherFunctions.defaultIndent()}return ${propSpec.name};"
            out += augCode.lineSeparator
            out += "}${augCode.lineSeparator}"
            out += "public void set${capitalized}(${propSpec.type} ${propSpec.name}) {"
            out += augCode.lineSeparator
            out += "${OtherFunctions.defaultIndent()}this.${propSpec.name} = ${propSpec.name};"
            out += augCode.lineSeparator
            out += "}${augCode.lineSeparator}"
            out += augCode.lineSeparator
        }
        final g = context.newGenCode()
        g.contentParts.add(context.newContent(out))
        g.replaceAugCodeDirectives = true
        g.indent = augCode.indent
        return g
    }

    static generateEqualsAndHashCode(augCode, context) {
        // don't override if empty.
        if (context.fileScope.theClassProps.size == 0) {
            return ''
        }
        
        def out = '';
        
        // generate equals() override
        out += "@Override${augCode.lineSeparator}"
        out += "public boolean equals(Object obj) {"
        out += augCode.lineSeparator
        out += "${OtherFunctions.defaultIndent()}if (!(obj instanceof ${context.fileScope.theClassName})) {"
        out += augCode.lineSeparator
        out += "${OtherFunctions.defaultIndent()}${OtherFunctions.defaultIndent()}return false;"
        out += augCode.lineSeparator
        out += "${OtherFunctions.defaultIndent()}" + '}'
        out += augCode.lineSeparator
        out += "${OtherFunctions.defaultIndent()}${context.fileScope.theClassName} other = (${context.fileScope.theClassName}) obj;"
        out += augCode.lineSeparator
        
        for (propSpec in context.fileScope.theClassProps) {
            if (Character.isUpperCase(propSpec.type[0] as char)) {
                out += OtherFunctions.defaultIndent()
                out += 'if (!Objects.equals(this.'
                out += propSpec.name;
                out += ', other.' 
                out += propSpec.name
                out += ')) {'
            }
            else {
                out += OtherFunctions.defaultIndent()
                out += 'if (this.'
                out += propSpec.name;
                out += ' != other.' 
                out += propSpec.name
                out += ') {'
            }
            out += augCode.lineSeparator
            out += "${OtherFunctions.defaultIndent()}${OtherFunctions.defaultIndent()}return false;"
            out += augCode.lineSeparator
            out += OtherFunctions.defaultIndent() + '}'
            out += augCode.lineSeparator
        }
        
        out += "${OtherFunctions.defaultIndent()}return true;${augCode.lineSeparator}"
        out += '}'
        out += augCode.lineSeparator
        out += augCode.lineSeparator
        
        // generate hashCode() override with Objects.hashCode()
        out += "@Override${augCode.lineSeparator}"
        out += "public int hashCode() {"
        out += augCode.lineSeparator
        if (context.fileScope.theClassProps.size == 1) {
            out += "${OtherFunctions.defaultIndent()}return Objects.hashCode("
            out += context.fileScope.theClassProps[0].name
        }
        else {
            out += "${OtherFunctions.defaultIndent()}return Objects.hash("
            for (def i = 0; i < context.fileScope.theClassProps.size; i++) {
                if (i > 0) {
                    out += ', '
                }
                out += context.fileScope.theClassProps[i].name
            }
        }
        out += ");${augCode.lineSeparator}"
        out += '}'
        out += augCode.lineSeparator
        final g = context.newGenCode()
        g.contentParts.add(context.newContent(out))
        g.replaceAugCodeDirectives = true
        g.indent = augCode.indent
        return g
    }

    static generateToString(augCode, context) {
        def out = '';
        out += "@Override${augCode.lineSeparator}"
        out += "public String toString() {"
        out += augCode.lineSeparator
        out += "${OtherFunctions.defaultIndent()}return String.format(getClass().getSimpleName() + "
        def exactOut = '"{';
        def outArgs = '';
        for (def i = 0; i < context.fileScope.theClassProps.size; i++) {
            if (i > 0) {
                exactOut += ', '
                outArgs += ', '
            }
            exactOut += context.fileScope.theClassProps[i].name + '=%s'
            outArgs += context.fileScope.theClassProps[i].name
        }
        exactOut += '}"'
        final g = context.newGenCode()
        g.contentParts.add(context.newContent(out));
        g.contentParts.add(context.newContent(exactOut, true))
        out = ''
        if (outArgs) {
            out += ",";
            out += augCode.lineSeparator;
            out += OtherFunctions.defaultIndent();
            out += OtherFunctions.defaultIndent();
        }
        out += outArgs
        out += ");${augCode.lineSeparator}"
        out += '}'
        out += augCode.lineSeparator
        g.contentParts.add(context.newContent(out))
        g.replaceAugCodeDirectives = true
        g.indent = augCode.indent
        return g
    }
}