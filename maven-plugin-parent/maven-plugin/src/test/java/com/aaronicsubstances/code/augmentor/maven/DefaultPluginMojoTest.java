package com.aaronicsubstances.code.augmentor.maven;

import org.testng.Reporter;
import org.testng.annotations.*;

import static org.testng.Assert.*;

import java.io.File;

public class DefaultPluginMojoTest extends AbstractPluginMojoTest {

    @Test
    public void testDefaultMojoWithMainClassGeneration() throws Exception {
        if (!isFunctionalTestsEnabled()) {
            return;
        }
        File projectDir = new File(getTestProjectsDir(), "default-mojo-project");
        assertTrue( projectDir.exists() );
        File output = File.createTempFile("stdout+err-", ".txt");
        int actualExitCode = launchCommand(projectDir, output, "mvn", true, 
            "codeaugmentor:run", "-e");
        String outputText = readText(output);
        assertEquals(actualExitCode, 0, outputText);
        Reporter.log(outputText);
    }
}