package com.aaronicsubstances.code.augmentor.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class CompletionTaskFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.aaronicsubstances.code-augmentor'
            }
        """
        File workingDir = testProjectDir.newFolder("build", "codeAugmentor")
        File testPrepFile = new File(workingDir, "prepResults.json")
        testPrepFile << """{ "encoding": "UTF-8" }
        """
        File genCodesFile = new File(workingDir, "genCodes.json")
        genCodesFile << """{}
        """
    }

    def "test codeAugmentorComplete task with extension defaults"() {
        buildFile << """
            codeAugmentor {
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorComplete', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorComplete").outcome == SUCCESS
    }
}