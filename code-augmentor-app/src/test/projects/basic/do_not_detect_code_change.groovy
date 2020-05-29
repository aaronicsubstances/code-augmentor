assert args.size() == 4
ant.code_aug_run(verbose: true, codeChangeDetectionDisabled: true) {
    srcDir(dir: 'src') {
        include(name: '**/*.java')
    }
}