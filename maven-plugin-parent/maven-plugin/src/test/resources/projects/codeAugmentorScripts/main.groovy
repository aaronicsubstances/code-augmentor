parentTask.execute({ args ->
    def functionName = args[0]
    binding.augCode = args[1]
    binding.context = args[2]
    evaluate(functionName + '(augCode, context)')
})