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
    private File defaultAugCodeFile;

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/prepResults.json", 
        readonly = true, required = true )
    private File defaultPrepFile;

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/genCodes.json", 
        readonly = true, required = true )
    private File defaultGenCodeFile;

    @Parameter( defaultValue = "//:GEN_CODE_START:", required = false )
    private String[] genCodeStartDirectives = new String[0];

    @Parameter( defaultValue = "//:GEN_CODE_END:", required = false )
    private String[] genCodeEndDirectives;

    @Parameter( defaultValue = "//:STR:", required = false )
    private String[] embeddedStringDirectives;

    @Parameter( defaultValue = "//:JSON:", required = false )
    private String[] embeddedJsonDirectives = new String[0];

    @Parameter( defaultValue = "//:SKIP_CODE_START:", required = false )
    private String[] skipCodeStartDirectives = new String[0];

    @Parameter( defaultValue = "//:SKIP_CODE_END:", required = false )
    private String[] skipCodeEndDirectives = new String[0];

    @Parameter( required = false )
    private File prepFile;

    @Parameter(required = false)
    private AugCodeDirectiveSpec[] augCodeSpecs = new AugCodeDirectiveSpec[0];
    
    @Parameter( required = false )
    private File[] generatedCodeFiles = new File[0];

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/generated", 
        required = false )
    private File destDir;

    @Parameter( defaultValue = "${project.build.directory}/codeAugmentor/changeSet.txt", 
        required = false )
    private File changeSetInfoFile;

    @Parameter( required = false )
    private int augCodeSpecIndex = 0;

    @Parameter( required = false )
    private int genCodeFileIndex = 0;

    @Parameter( defaultValue="main.groovy", required = false )
    private String groovyEntryScriptName;

    protected boolean isVerbose() {
        return verbose;
    }

    protected String getEncoding() {
        return encoding;
    }

    protected String[] getGenCodeStartDirectives() {
        return genCodeStartDirectives;
    }

    protected String[] getGenCodeEndDirectives() {
        return genCodeEndDirectives;
    }

    protected String[] getEmbeddedStringDirectives() {
        return embeddedStringDirectives;
    }

    protected String[] getEmbeddedJsonDirectives() {
        return embeddedJsonDirectives;
    }

    protected String[] getSkipCodeStartDirectives() {
        return skipCodeStartDirectives;
    }

    protected String[] getSkipCodeEndDirectives() {
        return skipCodeEndDirectives;
    }

    protected File getPrepFile() {
        if (prepFile == null) {
            prepFile = defaultPrepFile;
        }
        return prepFile;
    }

    protected AugCodeDirectiveSpec[] getAugCodeSpecs() {
        if (augCodeSpecs.length == 0) {
            AugCodeDirectiveSpec defaultValue = new AugCodeDirectiveSpec();
            defaultValue.setDestFile(defaultAugCodeFile);
            defaultValue.setDirectives(new String[]{ "//:AUG_CODE:" });
            augCodeSpecs = new AugCodeDirectiveSpec[]{ defaultValue };
        }
        return augCodeSpecs;
    }

    protected File[] getGeneratedCodeFiles() {
        if (generatedCodeFiles.length == 0) {
            generatedCodeFiles = new File[]{ defaultGenCodeFile };
        }
        return generatedCodeFiles;
    }

    protected File getDestDir() {
        return destDir;
    }

    protected File getChangeSetInfoFile() {
        return changeSetInfoFile;
    }

    protected int getAugCodeSpecIndex() {
        return augCodeSpecIndex;
    }

    protected int getGenCodeFileIndex() {
        return genCodeFileIndex;
    }

    protected String getGroovyEntryScriptName() {
        return groovyEntryScriptName;
    }

    protected File getDefaultAugCodeFile() {
        return defaultAugCodeFile;
    }

    protected File getDefaultPrepFile() {
        return defaultPrepFile;
    }

    protected File getDefaultGenCodeFile() {
        return defaultGenCodeFile;
    }
}