static generateMainClass(augCode, c) {
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
    def g = c.newGenCode()
    def out = g.contentParts
    String indent = ' ' * 4 
    if (pkgName) {
        out << c.newContent("package $pkgName;$newline$newline")
    }
    out << c.newContent("public class $simpleClassName {$newline$newline")
    out << c.newContent("${indent}public static void main(String[] args) {$newline")
    out << c.newContent("${indent * 2}System.out.println(\"Hello from CodeAugmentor!\");$newline")
    out << c.newContent(indent) << c.newContent("}$newline")
    out << c.newContent('}')
    return [ g ]
}