assert args.size() == 4
try {
    ant.code_aug_run(verbose: true) {
        srcDir(dir: 'src') {
            include(name: '**/*.java')
        }
    }
    ant.fail("expected build exception due to code change prescence")
}
catch (ignore) {}