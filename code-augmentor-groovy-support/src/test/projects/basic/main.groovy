assert args.size() == 2
ant.code_aug_run(verbose: true) {
    srcDir(dir: 'src') {
        include(name: '**/*.java')
        exclude(name: 'com/Main2.java')
    }
}