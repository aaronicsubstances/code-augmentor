package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

/**
 * Processes augmenting code into generated code using Groovy.
 *
 */
@Mojo(name = "process")
public class ProcessCodeMojo extends AbstractPluginMojo {
    private static final JsonSlurper JSON_PARSER = new JsonSlurper();

    @Parameter( required = false )
    private int augCodeSpecIndex = 0;

    @Parameter( required = false )
    private int genCodeFileIndex = 0;

    @Parameter( defaultValue="main.groovy", required = false )
    private String entryScriptName;

    @Parameter( required=true )
    private File scriptDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // validate
            AugCodeDirectiveSpec[] augCodeSpecs = getAugCodeSpecs();
            File[] genCodeFiles = getGeneratedCodeFiles();
            if (augCodeSpecIndex < 0 || augCodeSpecIndex >= augCodeSpecs.length) {
                throw new MojoFailureException(String.format(
                    "parameter 'augCodeSpecIndex' is invalid: %s is outside valid range " +
                    "of 0 ..< %s", augCodeSpecIndex, augCodeSpecs.length));
            }
            if (genCodeFileIndex < 0 || genCodeFileIndex >= genCodeFiles.length) {
                throw new MojoFailureException(String.format(
                    "parameter 'genCodeFileIndex' is invalid: %s is outside valid range " +
                    "of 0 ..< %s", genCodeFileIndex, genCodeFiles.length));
            }
            
            File augCodeFile = augCodeSpecs[augCodeSpecIndex].getDestFile();
            File genCodeFile = genCodeFiles[genCodeFileIndex];

            ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
            genericTask.setLogAppender(TaskUtils.createLogAppender(this, isVerbose()));
            genericTask.setInputFile(augCodeFile);
            genericTask.setOutputFile(genCodeFile);
            genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));
    
            URL[] scriptEngineRoots = new URL[]{ scriptDir.toURI().toURL() };
            GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
            CompilerConfiguration cc = new CompilerConfiguration();
            cc.setRecompileGroovySource(false);
            scriptEngine.setConfig(cc);
            Binding binding = new Binding();
            binding.setVariable("parentTask", genericTask);

            if (isVerbose()) {
                // print task properties - generic task ones, and any ones outside
                getLog().info("Configuration properties:");
                getLog().info("\taugCodeSpecIndex: " + augCodeSpecIndex);
                getLog().info("\tgenCodeFileIndex: " + genCodeFileIndex);
                getLog().info("\tscriptDir: " + scriptDir);
                getLog().info("\tentryScriptName: " + entryScriptName);
                getLog().info("\tgenericTask.inputFile: " + augCodeFile);
                getLog().info("\tgenericTask.outputFile: " + genCodeFile);
                getLog().info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
                getLog().info("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
            }

            getLog().info("Launching " + entryScriptName + "...");
            List<Throwable> scriptErrors = new ArrayList<>();
            try {
                scriptEngine.run(entryScriptName, binding);
            }
            catch (Throwable t) {
                scriptErrors.add(t);
            }

            scriptErrors.addAll(genericTask.getAllErrors());
    
            // fail build if there were errors.
            if (!scriptErrors.isEmpty()) {
                throw TaskUtils.convertToMavenException(scriptErrors);
            }
        }
        catch (Throwable ex) {
            if (ex instanceof MojoExecutionException) {
                throw (MojoExecutionException) ex;
            }
            if (ex instanceof MojoFailureException) {
                throw (MojoFailureException) ex;
            }
            throw new MojoFailureException("General plugin error: " + ex, ex);
        }
    }
}