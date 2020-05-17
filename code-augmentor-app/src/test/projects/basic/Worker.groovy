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
    assert g instanceof LinkedHashMap
    def out = g.contentParts
    String indent = ' ' * 4 
    if (pkgName) {
        out << context.newContent("package $pkgName;$newline$newline")
    }
    out << context.newContent("public class $simpleClassName {$newline$newline")
    out << context.newContent("${indent}public static void main(String[] args) {$newline")
    out << context.newContent("${indent * 2}System.out.println(\"Hello from CodeAugmentor!\");$newline")
    out << context.newContent(indent) << context.newContent("}$newline")
    out << context.newContent('}')
    return g
}