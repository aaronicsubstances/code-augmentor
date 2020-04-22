package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskExtensionFunction;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
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
public class ProcessingMojo extends AbstractPluginMojo {

    @Parameter( required=true )
    private File groovyScriptDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // validate
            int augCodeSpecIndex = getAugCodeSpecIndex();
            AugCodeDirectiveSpec[] augCodeSpecs = getAugCodeSpecs();
            if (augCodeSpecIndex < 0 || augCodeSpecIndex >= augCodeSpecs.length) {
                throw new MojoFailureException(String.format(
                    "parameter 'augCodeSpecIndex' is invalid: %s is outside valid range " +
                    "of 0 ..< %s", augCodeSpecIndex, augCodeSpecs.length));
            }
            int genCodeFileIndex = getGenCodeFileIndex();
            File[] genCodeFiles = getGeneratedCodeFiles();
            if (genCodeFileIndex < 0 || genCodeFileIndex >= genCodeFiles.length) {
                throw new MojoFailureException(String.format(
                    "parameter 'genCodeFileIndex' is invalid: %s is outside valid range " +
                    "of 0 ..< %s", genCodeFileIndex, genCodeFiles.length));
            }

            boolean resolvedVerbose = isVerbose();
            File resolvedAugCodeFile = null;
            if (augCodeSpecs[augCodeSpecIndex] != null) {
                resolvedAugCodeFile = augCodeSpecs[augCodeSpecIndex].getDestFile();
            }
            File resolvedGenCodeFile = genCodeFiles[genCodeFileIndex];
            String resolvedGroovyEntryScriptName = getGroovyEntryScriptName();

            completeExecute(this, resolvedVerbose, augCodeSpecIndex, genCodeFileIndex, 
                resolvedAugCodeFile, resolvedGenCodeFile, null, null, null,
                groovyScriptDir, resolvedGroovyEntryScriptName);
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

//:SKIP_CODE_START:
    private static final JsonSlurper JSON_PARSER = new JsonSlurper();
    
    static void completeExecute(AbstractMojo task, boolean resolvedVerbose,
            int resolvedAugCodeSpecIndex, int resolvedGenCodeFileIndex,
            File resolvedAugCodeFile, File resolvedGenCodeFile, 
            GenericTaskExtensionFunction resolvedScriptEvalFunction,
            List<String> resolvedStackTraceLimitPrefixes, 
            List<String> resolvedStackTraceFilterPrefixes,
            File resolvedGroovyScriptDir, String resolvedGroovyEntryScriptName) throws Exception {
        
        if (resolvedAugCodeFile == null) {
            if (task instanceof ProcessingMojo) {
                int i = resolvedAugCodeSpecIndex;
                throw new MojoExecutionException("invalid null value found at augCodeSpecs[" + i + "]?.destFile");
            }
            else {
                throw new RuntimeException("unexpected absence of augCodeFile");
            }
        }
        if (resolvedGenCodeFile == null) {
            if (task instanceof ProcessingMojo) {
                int i = resolvedGenCodeFileIndex;
                throw new MojoExecutionException("invaid null value found at generatedCodeFiles[" + i + "]");
            }
            else {
                throw new RuntimeException("unexpected absence of genCodeFile");
            }
        }
        // either eval function or groovy script dir is required.
        if (resolvedScriptEvalFunction == null && resolvedGroovyScriptDir == null) {
            throw new MojoExecutionException("groovyScriptDir property is required");
        }Log logger = task.getLog();
        ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setInputFile(resolvedAugCodeFile);
        genericTask.setOutputFile(resolvedGenCodeFile);
        genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));

        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            logger.info("Configuration properties:");
            if (task instanceof ProcessingMojo) {
                logger.info("\taugCodeSpecIndex: " + resolvedAugCodeSpecIndex);
                logger.info("\tgenCodeFileIndex: " + resolvedGenCodeFileIndex);
            }
            logger.info("\tgroovyScriptDir: " + resolvedGroovyScriptDir);
            logger.info("\tgroovyEntryScriptName: " + resolvedGroovyEntryScriptName);
            logger.info("\tgenericTask.inputFile: " + resolvedAugCodeFile);
            logger.info("\tgenericTask.outputFile: " + resolvedGenCodeFile);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            logger.info("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
        }

        List<Throwable> scriptErrors = new ArrayList<>();
        if (resolvedScriptEvalFunction == null) {
            URL[] scriptEngineRoots = new URL[]{ resolvedGroovyScriptDir.toURI().toURL() };
            GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
            CompilerConfiguration cc = new CompilerConfiguration();
            cc.setRecompileGroovySource(false);
            scriptEngine.setConfig(cc);
            Binding binding = new Binding();
            binding.setVariable("parentTask", genericTask);
            logger.info("Launching " + resolvedGroovyEntryScriptName + "...");
            scriptErrors = new ArrayList<>();
            try {
                scriptEngine.run(resolvedGroovyEntryScriptName, binding);
            }
            catch (Throwable t) {
                scriptErrors.add(t);
            }
        }
        else {
            try {
                genericTask.execute(resolvedScriptEvalFunction);
            }
            catch (Throwable t) {
                scriptErrors.add(t);
            }
        }

        scriptErrors.addAll(genericTask.getAllErrors());

        // fail build if there were errors.
        if (!genericTask.getAllErrors().isEmpty()) {
            String allExMsg = GenericTaskException.toExceptionMessageWithScriptConsideration(
                genericTask.getAllErrors(), true, 
                resolvedStackTraceLimitPrefixes, resolvedStackTraceFilterPrefixes);
            throw new MojoExecutionException(allExMsg);
        }
    }
//:SKIP_CODE_END:
}