package com.aaronicsubstances.code.augmentor.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class PreCodeAugmentationTaskFunctionalTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.aaronicsubstances.codeaugmentor'
            }
        """
    }

    def "test code_aug_prepare task with extension defaults"() {
        buildFile << """
            code_augmentor {
                fileSets.add(project.fileTree('src/main/java') {
                    include '**/*.java'
                })
                fileSets.add(project.fileTree('src/test/java') {
                    include '**/*.java'
                })
                fileSets.add(project.fileTree('src/main/groovy') {
                    include '**/*.groovy'
                })
                fileSets.add(project.fileTree('src/test/groovy') {
                    include '**/*.groovy'
                })
                fileSets.add(project.fileTree('src/main/kotlin') {
                    include '**/*.kt'
                })
                fileSets.add(project.fileTree('src/test/kotlin') {
                    include '**/*.kt'
                })
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('code_aug_prepare', '--stacktrace')
            .withPluginClasspath()
            .build()

        then:
        result.task(":code_aug_prepare").outcome == SUCCESS
    }
}