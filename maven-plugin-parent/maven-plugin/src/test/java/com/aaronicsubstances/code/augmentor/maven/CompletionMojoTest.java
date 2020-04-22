package com.aaronicsubstances.code.augmentor.maven;

import org.testng.Reporter;
import org.testng.annotations.*;

import static org.testng.Assert.*;

import java.io.File;

public class CompletionMojoTest extends AbstractPluginMojoTest {
    
    @Test
    public void testWithNoSrcFiles() throws Exception {
        if (!isFunctionalTestsEnabled()) {
            return;
        }
        File projectDir = new File(getTestProjectsDir(), "complete-mojo-project");
        assertTrue( projectDir.exists() );
        File output = File.createTempFile("stdout+err-", ".txt");
        int actualExitCode = launchCommand(projectDir, output, "mvn", true, 
            "codeaugmentor:complete", "-e");
        String outputText = readText(output);
        assertEquals(actualExitCode, 0, outputText);
        Reporter.log(outputText);
    }
}