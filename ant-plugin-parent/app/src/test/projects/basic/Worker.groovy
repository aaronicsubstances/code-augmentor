static generateMainClass(augCode, context) {
    def newline = augCode.lineSeparator
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
    def g = context.newGenCode()
    def out = g.contentParts
    String indent = ' ' * 4 
    if (pkgName) {
        out << g.newPart("package $pkgName;$newline$newline")
    }
    out << g.newPart("public class $simpleClassName {$newline$newline")
    out << g.newPart("${indent}public static void main(String[] args) {$newline")
    out << g.newPart("${indent * 2}System.out.println(\"Hello from CodeAugmentor!\");$newline")
    out << g.newPart(indent) << g.newPart("}$newline")
    out << g.newPart('}')
    return g
}

static testFunc(augCode, context) {
    println("It works")
}