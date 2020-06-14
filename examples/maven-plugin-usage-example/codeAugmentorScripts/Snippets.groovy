class Snippets {
    static generateClassContents(augCode, context) {
        def newline = augCode.lineSeparator; 
        def fieldSpecs = augCode.args[0].fields
        def g = context.newGenCode()
        def output = g.contentParts
        // generate fields.
        for (fieldSpec in fieldSpecs) {
            output << context.newContent("private ${fieldSpec.type} ${fieldSpec.name};$newline");
        }
        output << context.newContent(newline);
            
        // generate getter/setters
        def indent = ' ' * 4;
        for (fieldSpec in fieldSpecs) {
            def capitalized = "${fieldSpec.name[0]}".toUpperCase() + fieldSpec.name[1..-1];
            output << context.newContent("public ${fieldSpec.type} get${capitalized}() {$newline")
            output << context.newContent("${indent}return ${fieldSpec.name};$newline")
            output << context.newContent("}$newline$newline")
            output << context.newContent("public void set${capitalized}(${fieldSpec.type} ${fieldSpec.name}) {$newline")
            output << context.newContent("${indent}this.${fieldSpec.name} = ${fieldSpec.name};$newline")
            output << context.newContent("}$newline$newline");
        }
        return g
    }
}