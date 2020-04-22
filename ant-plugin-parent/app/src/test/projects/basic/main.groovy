ant.project.addReference("scriptEvalFunction", { args ->
    def functionName = args[0]
    binding.augCode = args[1]
    binding.context = args[2]
    return evaluate(functionName + '(augCode, context)')
})
ant.code_aug_run {
    srcDir(dir: 'src') {
        includes: '**/*.java'
    }
}