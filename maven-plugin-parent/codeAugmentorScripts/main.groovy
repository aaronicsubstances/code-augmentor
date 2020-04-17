// import scripts with functions.
// fortunately in Groovy, we only have to do this for scripts
// not in default package (just like this script).

final FUNCTION_NAME_REGEX = /^((Snippets)\.)[a-zA-Z]\w*$/
parentTask.execute({ args ->
    def functionName = args[0]
    binding.augCode = args[1]
    binding.context = args[2]
    if (functionName ==~ FUNCTION_NAME_REGEX) {
        return evaluate(functionName + '(augCode, context)')
    }
    else {
        throw new RuntimeException("Invalid/Unsupported function name: " + functionName)
    }
})