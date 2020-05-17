package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.net.URL;

import org.apache.maven.shared.utils.io.FileUtils;

public abstract class AbstractPluginMojoTest {
    private File testProjectsDir;
    
    public File getTestProjectsDir() {
        if (testProjectsDir == null) {
            URL locatorUrl = getClass().getResource("/projects");
            testProjectsDir = new File(locatorUrl.getFile());
        }
        return testProjectsDir;
    }

    public static boolean isFunctionalTestsEnabled() {
        return Boolean.parseBoolean(System.getProperty("alltests"));
    }

    public static String readTextAndDeleteFile(File f) {
        try {
            String s = FileUtils.fileRead(f);
            f.delete();
            return s;
        }
        catch (Exception ex) {
            return ex.toString();
        }
    }

    public static String getPluginPrefix() {
        return "code-augmentor";
    }
}