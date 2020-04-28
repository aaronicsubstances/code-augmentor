parentTask.execute({ functionName, augCode, context ->
    binding.codeAugmentorVariable_augCode = augCode
    binding.codeAugmentorVariable_context = context
    evaluate(functionName + '(codeAugmentorVariable_augCode, codeAugmentorVariable_context)')
})