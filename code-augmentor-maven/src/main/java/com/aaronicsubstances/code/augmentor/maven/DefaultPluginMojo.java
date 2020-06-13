package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

@Mojo(name = "run")
public class DefaultPluginMojo extends AbstractPluginMojo {

    @Parameter( required=true )
    public FileSet[] fileSets = new FileSet[0];

    @Parameter( required=true )
    private File groovyScriptDir;

    @Parameter( required = false, defaultValue = "//:AUG_CODE:")
    private String[] augCodeDirectives = new String[0];
    
    private static final JsonSlurper JSON_PARSER = new JsonSlurper();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // prepare...
            String resolvedEncoding = getEncoding();
            boolean resolvedVerbose = isVerbose();
            List<FileSet> resolvedFileSets = Arrays.asList(fileSets);
            List<String> resolvedGenCodeStartDirectives = Arrays.asList(getGenCodeStartDirectives());
            List<String> resolvedGenCodeEndDirectives = Arrays.asList(getGenCodeEndDirectives());
            List<String> resolvedEmbeddedStringDirectives = Arrays.asList(getEmbeddedStringDirectives());
            List<String> resolvedEmbeddedJsonDirectives = Arrays.asList(getEmbeddedJsonDirectives());
            List<String> resolvedSkipCodeStartDirectives = Arrays.asList(getSkipCodeStartDirectives());
            List<String> resolvedSkipCodeEndDirectives = Arrays.asList(getSkipCodeEndDirectives());
            List<String> resolvedInlineGenCodeDirectives = Arrays.asList(getInlineGenCodeDirectives());
            List<String> resolvedNestedLevelStartMarkers = Arrays.asList(getNestedLevelStartMarkers());
            List<String> resolvedNestedLevelLEndMarkers = Arrays.asList(getNestedLevelEndMarkers());

            File resolvedPrepFile = getDefaultPrepFile();
            File resolvedAugCodeFile = getDefaultAugCodeFile();
            File resolvedGenCodeFile = getDefaultGenCodeFile();
            List<List<String>> resolvedAugCodeSpecDirectives = Arrays.asList(
                Arrays.asList(augCodeDirectives));
            List<File> resolvedAugCodeFiles = Arrays.asList(resolvedAugCodeFile);
            PreparationMojo.completeExecute(this, resolvedEncoding, resolvedVerbose, resolvedFileSets, 
                resolvedGenCodeStartDirectives, resolvedGenCodeEndDirectives, 
                resolvedEmbeddedStringDirectives, resolvedEmbeddedJsonDirectives, 
                resolvedSkipCodeStartDirectives, resolvedSkipCodeEndDirectives, 
                resolvedAugCodeSpecDirectives, resolvedAugCodeFiles, 
                resolvedPrepFile,
                resolvedInlineGenCodeDirectives,
                resolvedNestedLevelStartMarkers, resolvedNestedLevelLEndMarkers);

            // process...
            String resolvedGroovyEntryScriptName = getGroovyEntryScriptName();
            executeProcessStage(resolvedVerbose, 
                resolvedAugCodeFile, resolvedGenCodeFile,
                groovyScriptDir, resolvedGroovyEntryScriptName);

            // complete...
            List<File> resolvedGenCodeFiles = Arrays.asList(resolvedGenCodeFile);
            File resolvedDestDir = getDestDir();
            boolean resolvedCodeChangeDetectionDisabled = getCodeChangeDetectionDisabled();
            boolean resolvedFailOnChanges = getFailOnChanges();
            CompletionMojo.completeExecute(this, resolvedVerbose, resolvedPrepFile, 
                resolvedGenCodeFiles, resolvedDestDir, resolvedCodeChangeDetectionDisabled,
                resolvedFailOnChanges);

        }
        catch (MojoExecutionException ex) {
            throw ex;
        }
        catch (MojoFailureException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new MojoFailureException("General plugin error: " + ex, ex);
        }
    }
    
    private void executeProcessStage(boolean resolvedVerbose,
            File resolvedAugCodeFile, File resolvedGenCodeFile,
            File resolvedGroovyScriptDir, String resolvedGroovyEntryScriptName) throws Exception {
        
        if (resolvedAugCodeFile == null) {
            throw new RuntimeException("unexpected absence of augCodeFile");
        }
        if (resolvedGenCodeFile == null) {
            throw new RuntimeException("unexpected absence of genCodeFile");
        }
        // groovy script dir is required.
        if (resolvedGroovyScriptDir == null) {
            throw new MojoExecutionException("groovyScriptDir property is required");
        }
        
        Log logger = getLog();
        ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
        genericTask.setLogAppender(TaskUtils.createLogAppender(this, resolvedVerbose));
        genericTask.setInputFile(resolvedAugCodeFile);
        genericTask.setOutputFile(resolvedGenCodeFile);
        genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));

        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            logger.info("Configuration properties:");
            logger.info("\tgroovyScriptDir: " + resolvedGroovyScriptDir);
            logger.info("\tgroovyEntryScriptName: " + resolvedGroovyEntryScriptName);
            logger.info("\tgenericTask.inputFile: " + resolvedAugCodeFile);
            logger.info("\tgenericTask.outputFile: " + resolvedGenCodeFile);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            logger.info("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
        }

        URL[] scriptEngineRoots = new URL[]{ resolvedGroovyScriptDir.toURI().toURL() };
        GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setRecompileGroovySource(false);
        scriptEngine.setConfig(cc);
        Binding binding = new Binding();
        binding.setVariable("parentTask", genericTask);
        logger.info("Launching " + resolvedGroovyEntryScriptName + "...");        
        List<Throwable> scriptErrors = new ArrayList<>();
        try {
            scriptEngine.run(resolvedGroovyEntryScriptName, binding);
        }
        catch (Throwable t) {
            scriptErrors.add(t);
        }

        scriptErrors.addAll(genericTask.getAllErrors());

        // fail build if there were errors.
        if (!scriptErrors.isEmpty()) {
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                scriptErrors, true, null, null);
            throw new MojoExecutionException(allExMsg);
        }
    }
}