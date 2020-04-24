println "args: $args"
ant.code_aug_run {
    srcDir(dir: 'src') {
        includes: '**/*.java'
    }
}