class Snippets {
    static generateClassContents(augCode, context) {
        def newline = System.lineSeparator(); 
        def fieldSpecs = augCode.args[0].fields
        def output = new StringBuilder()
        // generate fields.
        for (fieldSpec in fieldSpecs) {
            output << "private ${fieldSpec.type} ${fieldSpec.name};" << newline;
        }
        output << newline;
            
        // generate getter/setters
        def indent = ' ' * 4;
        for (fieldSpec in fieldSpecs) {
            def capitalized = "${fieldSpec.name[0]}".toUpperCase() + fieldSpec.name[1..-1];
            output << "public ${fieldSpec.type} get${capitalized}() {" << newline;
            output << "${indent}return ${fieldSpec.name};" << newline;
            output << "}" << newline << newline;
            output << "public void set${capitalized}(${fieldSpec.type} ${fieldSpec.name}) {" << newline;
            output << "${indent}this.${fieldSpec.name} = ${fieldSpec.name};" << newline;
            output << "}" << newline << newline;
        }
        return output.toString();
    }
}