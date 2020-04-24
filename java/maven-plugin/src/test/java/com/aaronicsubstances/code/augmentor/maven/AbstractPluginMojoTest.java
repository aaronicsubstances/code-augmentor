package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.utils.io.FileUtils;

public abstract class AbstractPluginMojoTest {
    private File testProjectsDir;
    
    public File getTestProjectsDir() {
        if (testProjectsDir == null) {
            URL locatorUrl = getClass().getResource("/projects/locator.txt");
            testProjectsDir = new File(locatorUrl.getFile()).getParentFile();
        }
        return testProjectsDir;
    }

    public static int launchCommand(File workingDir, File output, 
            String cmdPath, boolean isBatchCmdOnWindows, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(workingDir);
        List<String> fullCmd = new ArrayList<>();
        fullCmd.add(cmdPath);
        fullCmd.addAll(Arrays.asList(args));
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            fullCmd.addAll(0, Arrays.asList("cmd", "/c"));
        }
        pb.directory(workingDir).command(fullCmd);
        if (output != null) {
            pb.redirectError(output).redirectOutput(output);
        }
        Process proc = pb.start();
        proc.waitFor(1, TimeUnit.MINUTES);
        return proc.exitValue();
    }

    public static boolean isFunctionalTestsEnabled() {
        return Boolean.parseBoolean(System.getProperty("codeaugmentor.functionaltests.run"));
    }

    public static String readText(File f) {
        try {
            return FileUtils.fileRead(f);
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }
}