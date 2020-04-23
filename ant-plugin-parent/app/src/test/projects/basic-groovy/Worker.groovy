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
    def out = new StringBuilder()
    String indent = ' ' * 4 
    if (pkgName) {
        out << "package $pkgName;$newline$newline"
    }
    out << "public class $simpleClassName {$newline$newline"
    out << "${indent}public static void main(String[] args) {$newline"
    out << "${indent * 2}System.out.println(\"Hello from CodeAugmentor!\");$newline"
    out << indent << "}$newline"
    out << '}'
    return context.newContent("$out", true)
}