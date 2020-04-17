package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractPluginMojo extends AbstractMojo {
    @Parameter( required = false)
    private boolean verbose = false;

    @Parameter( defaultValue="${project.build.sourceEncoding}", readonly=true, required=true )
    private String encoding;

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/augCodes.json", 
        readonly = true, required = true )
    private File defaultDestFile;

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/prepResults.json", 
        required = false )
    private File prepFile;

    @Parameter(required = false)
    private AugCodeDirectiveSpec[] augCodeSpecs = new AugCodeDirectiveSpec[0];
    
    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/genCodes.json",
        required = false )
    private File[] generatedCodeFiles = new File[0];

    protected boolean isVerbose() {
        return verbose;
    }

    protected String getEncoding() {
        return encoding;
    }

    protected File getPrepFile() {
        return prepFile;
    }

    protected AugCodeDirectiveSpec[] getAugCodeSpecs() {
        if (augCodeSpecs.length == 0) {
            AugCodeDirectiveSpec defaultValue = new AugCodeDirectiveSpec();
            defaultValue.setDestFile(defaultDestFile);
            defaultValue.setDirectives(new String[]{ "//:AUG_CODE:" });
            augCodeSpecs = new AugCodeDirectiveSpec[]{ defaultValue };
        }
        return augCodeSpecs;
    }

    protected File[] getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }
}