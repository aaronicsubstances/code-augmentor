static generateClassContents(augCode, context) {
    def newline = augCode.lineSeparator; 
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
    return "$output";
}

static generateMainClass(augCode, context) {
    def newline = augCode.lineSeparator; 
    def className = augCode.args[0].trim()
    // split class name if it has package name in it.
    def simpleClassName = className
    def pkgName = null
    int periodIndex = className.lastIndexOf('.')
    if (periodIndex != -1) {
        pkgName = className[0 ..< periodIndex]
        simpleClassName = className[periodIndex + 1 .. -1]
    }
    // now generate main class file contents
    StringBuilder out = new StringBuilder()
    String indent = ' ' * 4
    if (pkgName) {
        out << 'package ' << pkgName << ';' << newline << newline
    }
    out << 'public class ' << simpleClassName << ' {' << newline << newline
    out << indent << 'public static void main(String[] args) {' << newline
    out << indent * 2 << 'System.out.println("Hello from CodeAugmentor!");' << newline
    out << indent << '}' << newline
    out << '}'
    return "$output"
}
