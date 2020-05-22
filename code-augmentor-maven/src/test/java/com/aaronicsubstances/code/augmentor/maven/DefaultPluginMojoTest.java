package com.aaronicsubstances.code.augmentor.maven;

import org.testng.Reporter;
import org.testng.annotations.*;

import static org.testng.Assert.*;

import java.io.File;

import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;

public class DefaultPluginMojoTest extends AbstractPluginMojoTest {

    @Test
    public void testDefaultMojoWithMainClassGeneration() throws Exception {
        if (!isFunctionalTestsEnabled()) {
            return;
        }
        runTest("do-not-detect-code-change", 0);
        runTest("code-change-absent", 0);
        runTest("code-change-present", 1);
        runTest("code-change-present-DF", 0);
    }

    private void runTest(String executionId, int expectedExitCode) throws Exception {
        File projectDir = new File(getTestProjectsDir(), "default-mojo-project");
        assertTrue( projectDir.exists() );
        File output = File.createTempFile("stdout+err-", ".txt");
        int actualExitCode = PluginUtils.execCommand(projectDir, output, output, "mvn", true, 
            "clean", getPluginPrefix() + ":run@" + executionId, "-e");
        String outputText = readTextAndDeleteFile(output);
        assertEquals(actualExitCode, expectedExitCode, outputText);
        Reporter.log(outputText);
    }
}