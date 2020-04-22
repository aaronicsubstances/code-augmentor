class Snippets {
    static generateClassContents(augCode, context) {
        def newline = augCode.lineSeparator; 
        def fieldSpecs = augCode.args[0].fields
        def g = context.newGenCode()
        def output = g.contentParts
        // generate fields.
        for (fieldSpec in fieldSpecs) {
            output << g.newPart("private ${fieldSpec.type} ${fieldSpec.name};$newline");
        }
        output << g.newPart(newline);
            
        // generate getter/setters
        def indent = ' ' * 4;
        for (fieldSpec in fieldSpecs) {
            def capitalized = "${fieldSpec.name[0]}".toUpperCase() + fieldSpec.name[1..-1];
            output << g.newPart("public ${fieldSpec.type} get${capitalized}() {$newline")
            output << g.newPart("${indent}return ${fieldSpec.name};$newline")
            output << g.newPart("}$newline$newline")
            output << g.newPart("public void set${capitalized}(${fieldSpec.type} ${fieldSpec.name}) {$newline")
            output << g.newPart("${indent}this.${fieldSpec.name} = ${fieldSpec.name};$newline")
            output << g.newPart("}$newline$newline");
        }
        return g
    }
}