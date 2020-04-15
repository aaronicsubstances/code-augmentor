package com.aaronicsubstances.code.augmentor.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class CodeAugmentationTaskFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.aaronicsubstances.codeaugmentor'
            }
        """
        File workingDir = testProjectDir.newFolder("build", "codeAugmentor")
        File testPrepFile = new File(workingDir, "prepResults.json")
        testPrepFile << """{}
        """
        File genCodesFile = new File(workingDir, "genCodes.json")
        genCodesFile << """{}
        """
    }

    def "test codeAugmentorGenerate task with extension defaults"() {
        buildFile << """
            codeAugmentor {
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorGenerate', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorGenerate").outcome == SUCCESS
    }
}