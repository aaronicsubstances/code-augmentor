package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;

@Mojo(name = "run")
public class DefaultPluginMojo extends AbstractPluginMojo {

    @Parameter( required=true )
    public FileSet[] fileSets = new FileSet[0];

    @Parameter( required=true )
    private File groovyScriptDir;

    @Parameter( required = false, defaultValue = "//:AUG_CODE:")
    private String[] augCodeDirectives = new String[0];

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
                resolvedPrepFile);

            // process...
            String resolvedGroovyEntryScriptName = getGroovyEntryScriptName();
            ProcessingMojo.completeExecute(this, resolvedVerbose, 0, 0, 
                resolvedAugCodeFile, resolvedGenCodeFile, null, null, null,
                groovyScriptDir, resolvedGroovyEntryScriptName);

            // complete...
            List<File> resolvedGenCodeFiles = Arrays.asList(resolvedGenCodeFile);
            File resolvedDestDir = getDestDir();
            File resolvedChangeSetInfoFile = getChangeSetInfoFile();
            CompletionMojo.completeExecute(this, resolvedEncoding, resolvedVerbose, resolvedPrepFile, 
                resolvedGenCodeFiles, resolvedDestDir, resolvedChangeSetInfoFile);

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
}