parentTask.execute({ functionName, augCode, context ->
    binding.augCode = augCode
    binding.context = context
    evaluate(functionName + '(augCode, context)')
})