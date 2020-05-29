assert args.size() == 4
ant.code_aug_run(verbose: true, failOnChanges: false) {
    srcDir(dir: 'src') {
        include(name: '**/*.java')
    }
}