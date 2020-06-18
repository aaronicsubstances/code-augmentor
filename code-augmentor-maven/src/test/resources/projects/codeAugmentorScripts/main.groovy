parentTask.validationCallback = { functionName, augCode, context -> null }
parentTask.evalFunction = { functionName, augCode, context ->
    binding.augCode = augCode
    binding.context = context
    evaluate(functionName + '(augCode, context)')
}
parentTask.execute()