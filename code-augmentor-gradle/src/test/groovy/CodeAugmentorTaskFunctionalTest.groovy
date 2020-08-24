package com.aaronicsubstances.code.augmentor.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class CodeAugmentorTaskFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File scriptDir
    File srcFolder
    File undeletedWorkingDir, undeletedDestDir

    static quoteRegex(p) {
        return p.replace("/", "\\/")
    }

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.aaronicsubstances.code-augmentor'
            }
        """
        scriptDir = testProjectDir.newFolder("scripts")
        File entryFile = new File(scriptDir, "main.groovy")
        entryFile << """
        parentTask.execute({ functionName, augCode, context ->
            binding.augCode = augCode
            binding.context = context
            evaluate(functionName + '(augCode, context)')
        })
        """
        File workerFile = new File(scriptDir, "Worker.groovy")
        workerFile << '''static generateMainClass(augCode, c) {
            def className = augCode.args[0].trim()
            // split class name if it has package name in it.
            def simpleClassName = className
            def pkgName = null
            int periodIndex = className.lastIndexOf('.')
            if (periodIndex != -1) {
                pkgName = className[0 ..< periodIndex]
                simpleClassName = className[periodIndex + 1 .. -1]
            }
            // now generate main class file contents
            def g = c.newGenCode()
            def out = g.contentParts
            String indent = ' ' * 4 
            if (pkgName) {
                out << c.newContent("package $pkgName;\\n\\n")
            }
            out << c.newContent("public class $simpleClassName {\\n\\n")
            out << c.newContent("${indent}public static void main(String[] args) {\\n")
            out << c.newContent("${indent * 2}System.out.println(\\"Hello from CodeAugmentor!\\");\\n")
            out << c.newContent(indent) << c.newContent('}\\n')
            out << c.newContent('}')
            return g
        }
        '''
        srcFolder = testProjectDir.newFolder("src")
        File srcPkgFolder = testProjectDir.newFolder("src", "com")
        File mainSrcFile = new File(srcPkgFolder, "Main.java")
        mainSrcFile << """//:AUG_CODE: Worker.generateMainClass
        |//:STR: com.Main
        |//:GEN_CODE_START:
        |package com;
        |
        |public class Main {
        |
        |    public static void main(String[] args) {
        |        System.out.println("Hello from CodeAugmentor!");
        |    }
        |}
        |//:GEN_CODE_END:
        |""".stripMargin()

        File main2SrcFile = new File(srcPkgFolder, "Main2.java")
        main2SrcFile << """//:AUG_CODE: Worker.generateMainClass
        |//:STR: com.Main2
        |//:GEN_CODE_START:
        |package com;
        |
        |public class Main {
        |
        |    public static void main(String[] args) {
        |        System.out.println("Hello from CodeAugmentor!");
        |    }
        |}
        |//:GEN_CODE_END:
        |""".stripMargin()

        undeletedWorkingDir = new File(System.getProperty("java.io.tmpdir"), 
            "CodeAugmentationTaskFunctionalTest")
        undeletedWorkingDir.mkdir()
        new File(undeletedWorkingDir, "Main-copy.java").bytes = mainSrcFile.bytes
        new File(undeletedWorkingDir, "Main2-copy.java").bytes = main2SrcFile.bytes
        new File(undeletedWorkingDir, "Worker-copy.groovy").bytes = workerFile.bytes
        undeletedDestDir = new File(undeletedWorkingDir, "generated")
        undeletedDestDir.mkdir()
    }

    def "test codeAugmentorRun task: code change detection disabled"() {
        buildFile << """
            codeAugmentor {
                fileSets.add(project.fileTree('${srcFolder.name}') {
                    include '**/*.java'
                })
                groovyScriptDir = "${scriptDir.name}"
                destDir = /${quoteRegex(undeletedDestDir.toString())}/
                codeChangeDetectionDisabled = true
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorRun', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorRun").outcome == TaskOutcome.SUCCESS
    }

    def "test codeAugmentorRun task: code changes present"() {
        buildFile << """
            codeAugmentor {
                fileSets.add(project.fileTree('${srcFolder.name}') {
                    include '**/*.java'
                })
                groovyScriptDir = "${scriptDir.name}"
                destDir = /${quoteRegex(undeletedDestDir.toString())}/
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorRun', '--stacktrace')
            .withPluginClasspath()
            .buildAndFail()

        then:
        result.task(":codeAugmentorRun").outcome == TaskOutcome.FAILED
    }

    def "test codeAugmentorRun task: code changes present - DF"() {
        buildFile << """
            codeAugmentor {
                fileSets.add(project.fileTree('${srcFolder.name}') {
                    include '**/*.java'
                })
                groovyScriptDir = "${scriptDir.name}"
                destDir = /${quoteRegex(undeletedDestDir.toString())}/
                failOnChanges = false
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorRun', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorRun").outcome == TaskOutcome.SUCCESS
    }

    def "test codeAugmentorRun task: code changes absent"() {
        buildFile << """
            codeAugmentor {
                fileSets.add(project.fileTree('${srcFolder.name}') {
                    include '**/*.java'
                    exclude 'com/Main2.java'
                })
                groovyScriptDir = "${scriptDir.name}"
                destDir = /${quoteRegex(undeletedDestDir.toString())}/
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorRun', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorRun").outcome == TaskOutcome.SUCCESS
    }
}
