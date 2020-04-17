package com.aaronicsubstances.code.augmentor.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ProcessCodeTaskFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File scriptDir

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.aaronicsubstances.codeaugmentor'
            }
        """
        File workingDir = testProjectDir.newFolder("build", "codeAugmentor")
        File augCodesFile = new File(workingDir, "augCodes.json")
        augCodesFile << """{}
        """
        scriptDir = testProjectDir.newFolder("scripts")
        File entryFile = new File(scriptDir, "main.groovy")
        entryFile << """
        parentTask.execute()
        """
    }

    def "test codeAugmentorProcess task with extension defaults"() {
        buildFile << """
            codeAugmentor {
                scriptDir = "${scriptDir.name}"
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorProcess', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorProcess").outcome == SUCCESS
    }
}