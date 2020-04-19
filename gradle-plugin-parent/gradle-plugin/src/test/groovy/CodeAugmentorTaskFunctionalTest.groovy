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
        parentTask.execute({ args ->
            def functionName = args[0]
            binding.augCode = args[1]
            binding.context = args[2]
            evaluate(functionName + '(augCode, context)')
        })
        """
        File workerFile = new File(scriptDir, "Worker.groovy")
        workerFile << """static generateMainClass(augCode, context) {
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
            StringBuilder out = new StringBuilder()
            String indent = ' ' * 4 
            if (pkgName) {
                out << 'package ' << pkgName << ';' << '\\n\\n'
            }
            out << 'public class ' << simpleClassName << ' {\\n\\n'
            out << indent << 'public static void main(String[] args) {\\n'
            out << indent * 2 << 'System.out.println("Hello from CodeAugmentor!");' << '\\n'
            out << indent << '}\\n'
            out << '}'
            return out.toString()
        }
        """
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