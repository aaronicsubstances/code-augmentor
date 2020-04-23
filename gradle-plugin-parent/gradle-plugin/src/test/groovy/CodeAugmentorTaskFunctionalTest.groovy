package com.aaronicsubstances.code.augmentor.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class CodeAugmentorTaskFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File scriptDir
    File srcFolder
    File undeletedWorkingDir, undeletedDestDir

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.aaronicsubstances.codeaugmentor'
            }
        """
        scriptDir = testProjectDir.newFolder("scripts")
        File entryFile = new File(scriptDir, "main.groovy")
        entryFile << """
        parentTask.execute({ functionName, augCode, context ->
            binding.codeAugmentorVariable_augCode = augCode
            binding.codeAugmentorVariable_context = context
            evaluate(functionName + '(codeAugmentorVariable_augCode, codeAugmentorVariable_context)')
        })
        """
        File workerFile = new File(scriptDir, "Worker.groovy")
        workerFile << '''static generateMainClass(augCode, context) {
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
            def g = context.newGenCode()
            def out = g.contentParts
            String indent = ' ' * 4 
            if (pkgName) {
                out << g.newPart("package $pkgName;\\n\\n")
            }
            out << g.newPart("public class $simpleClassName {\\n\\n")
            out << g.newPart("${indent}public static void main(String[] args) {\\n")
            out << g.newPart("${indent * 2}System.out.println(\\"Hello from CodeAugmentor!\\");\\n")
            out << g.newPart(indent) << g.newPart('}\\n')
            out << g.newPart('}')
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

        undeletedWorkingDir = new File(System.getProperty("java.io.tmpdir"), 
            "CodeAugmentationTaskFunctionalTest")
        undeletedWorkingDir.mkdir()
        new File(undeletedWorkingDir, "Main-copy.java").bytes = mainSrcFile.bytes
        new File(undeletedWorkingDir, "Worker-copy.groovy").bytes = workerFile.bytes
        undeletedDestDir = new File(undeletedWorkingDir, "generated")
        undeletedDestDir.mkdir()
    }

    def "test codeAugmentorRun task with main class generation"() {
        buildFile << """
            codeAugmentor {
                fileSets.add(project.fileTree('${srcFolder.name}') {
                    include '**/*.java'
                })
                groovyScriptDir = "${scriptDir.name}"
                destDir = /${undeletedDestDir}/
                changeSetInfoFile = /${new File(undeletedWorkingDir, 'changeSetInfoFile.txt')}/
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorRun', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorRun").outcome == SUCCESS
    }
}