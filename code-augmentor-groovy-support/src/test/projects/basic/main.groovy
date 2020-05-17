println "args: $args"
ant.code_aug_run(verbose: true) {
    srcDir(dir: 'src') {
        includes: '**/*.java'
    }
}