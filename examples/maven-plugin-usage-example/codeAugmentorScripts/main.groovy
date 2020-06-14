// import scripts with functions.
// fortunately in Groovy, we only have to do this for scripts
// not in default package (just like this script).

final FUNCTION_NAME_REGEX = /^((Snippets)\.)[a-zA-Z]\w*$/
parentTask.execute({ functionName, augCode, context ->
    binding.augCode = augCode
    binding.context = context
    if (functionName ==~ FUNCTION_NAME_REGEX) {
        return evaluate(functionName + '(augCode, context)')
    }
    else {
        throw new RuntimeException("Invalid/Unsupported function name: " + functionName)
    }
})