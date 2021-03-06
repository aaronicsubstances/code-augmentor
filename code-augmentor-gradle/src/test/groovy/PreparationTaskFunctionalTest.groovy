package com.aaronicsubstances.code.augmentor.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class PreparationTaskFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File srcFolder

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.aaronicsubstances.code-augmentor'
            }
        """
        srcFolder = testProjectDir.newFolder("src")
    }

    def "test codeAugmentorPrepare task with extension defaults"() {
        buildFile << """
            codeAugmentor {
                fileSets.add(project.fileTree('${srcFolder.name}') {
                    include '**/*.java'
                })
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('codeAugmentorPrepare', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":codeAugmentorPrepare").outcome == SUCCESS
    }
}