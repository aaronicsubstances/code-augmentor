package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.tasks.AugCodeProcessingSpec;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

/**
 * Prepares for code generation.
 *
 */
@Mojo( name = "prepare")
public class PreparationMojo extends AbstractPluginMojo {

    @Parameter( required=true )
    public FileSet[] fileSets = new FileSet[0];

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            String resolvedEncoding = getEncoding();
            boolean resolvedVerbose = isVerbose();
            List<FileSet> resolvedFileSets = Arrays.asList(fileSets);
            List<String> resolvedGenCodeStartDirectives = Arrays.asList(getGenCodeStartDirectives());
            List<String> resolvedGenCodeEndDirectives = Arrays.asList(getGenCodeEndDirectives());
            List<String> resolvedEmbeddedStringDirectives = Arrays.asList(getEmbeddedStringDirectives());
            List<String> resolvedEmbeddedJsonDirectives = Arrays.asList(getEmbeddedJsonDirectives());
            List<String> resolvedSkipCodeStartDirectives = Arrays.asList(getSkipCodeStartDirectives());
            List<String> resolvedSkipCodeEndDirectives = Arrays.asList(getSkipCodeEndDirectives());

            List<List<String>> resolvedAugCodeSpecDirectives = new ArrayList<>();
            List<File> resolvedAugCodeFiles = new ArrayList<>();
            AugCodeDirectiveSpec[] augCodeDirectiveSpecs = getAugCodeSpecs();
            for (AugCodeDirectiveSpec spec : augCodeDirectiveSpecs) {
                List<String> augCodeDirectives = null;
                File augCodeFile = null;
                if (spec != null) {
                    if (spec.getDirectives() != null) {
                        augCodeDirectives = Arrays.asList(spec.getDirectives());
                    }
                    augCodeFile = spec.getDestFile();
                }
                resolvedAugCodeSpecDirectives.add(augCodeDirectives);
                resolvedAugCodeFiles.add(augCodeFile);
            }
            File resolvedPrepFile = getPrepFile();
            completeExecute(this, resolvedEncoding, resolvedVerbose, resolvedFileSets, 
                resolvedGenCodeStartDirectives, resolvedGenCodeEndDirectives, 
                resolvedEmbeddedStringDirectives, resolvedEmbeddedJsonDirectives, 
                resolvedSkipCodeStartDirectives, resolvedSkipCodeEndDirectives, 
                resolvedAugCodeSpecDirectives, resolvedAugCodeFiles, 
                resolvedPrepFile);
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
    static void completeExecute(
            AbstractMojo task,
            String resolvedEncoding, boolean resolvedVerbose, 
            List<FileSet> resolvedFileSets,
            List<String> resolvedGenCodeStartDirectives,
            List<String> resolvedGenCodeEndDirectives,
            List<String> resolvedEmbeddedStringDirectives,
            List<String> resolvedEmbeddedJsonDirectives,
            List<String> resolvedSkipCodeStartDirectives,
            List<String> resolvedSkipCodeEndDirectives,
            List<List<String>> resolvedAugCodeSpecDirectives,
            List<File> resolvedAugCodeFiles,
            File resolvedPrepFile) throws Exception {
        
        if (resolvedGenCodeStartDirectives.isEmpty()) {
            throw new MojoExecutionException("at least 1 element is required in genCodeStartDirectives");
        }
        if (resolvedGenCodeEndDirectives.isEmpty()) {
            throw new MojoExecutionException("at least 1 element is required in genCodeEndDirectives");
        }
        if (resolvedAugCodeSpecDirectives.isEmpty()) {
            if (task instanceof PreparationMojo) {
                throw new MojoExecutionException("at least 1 element is required in augCodeSpecs");
            }
            else {
                throw new RuntimeException("unexpected absence of augCodeDirectives");
            }
        }
        // validate
        if (resolvedFileSets.isEmpty()) {
            throw new MojoExecutionException("at least 1 element is required in fileSets");
        }
        Charset charset = Charset.forName(resolvedEncoding); // validate encoding.
        for (int i = 0; i < resolvedAugCodeSpecDirectives.size(); i++) {
            List<String> resolvedAugCodeDirectives = resolvedAugCodeSpecDirectives.get(i);
            if (resolvedAugCodeDirectives.isEmpty()) {
                if (task instanceof PreparationMojo) {
                    throw new MojoExecutionException("at least 1 element is required in augCodeSpecs[" + i + "].directives");
                }
                else {
                    throw new MojoExecutionException("at least 1 element is required in augCodeDirectives");
                }
            }
        }
        for (int i = 0; i < resolvedAugCodeFiles.size(); i++) {
            File resolvedAugCodeFile = resolvedAugCodeFiles.get(i);
            if (resolvedAugCodeFile == null) {
                if (task instanceof PreparationMojo) {
                    throw new MojoExecutionException("invalid null value found at augCodeSpecs[" + i + "]?.destFile");
                }
                else {
                    throw new RuntimeException("unexpected absence of augCodeFile");
                }
            }
        }
        for (int i = 0; i < resolvedFileSets.size(); i++) {
            if (resolvedFileSets.get(i) == null) {
                throw new MojoExecutionException("invalid null value found at fileSets[" + i + "]");
            }
        }

        // Ensure uniqueness across directives.
        Set<String> allDirectives = new HashSet<>();
        int totalDirectiveCount = 0;
        allDirectives.addAll(resolvedGenCodeStartDirectives);
        totalDirectiveCount += resolvedGenCodeStartDirectives.size();
        allDirectives.addAll(resolvedGenCodeEndDirectives);
        totalDirectiveCount += resolvedGenCodeEndDirectives.size();
        allDirectives.addAll(resolvedEmbeddedStringDirectives);
        totalDirectiveCount += resolvedEmbeddedStringDirectives.size();
        allDirectives.addAll(resolvedEmbeddedJsonDirectives);
        totalDirectiveCount += resolvedEmbeddedJsonDirectives.size();
        allDirectives.addAll(resolvedSkipCodeStartDirectives);
        totalDirectiveCount += resolvedSkipCodeStartDirectives.size();
        allDirectives.addAll(resolvedSkipCodeEndDirectives);
        totalDirectiveCount += resolvedSkipCodeEndDirectives.size();
        
        for (List<String> resolvedAugCodeDirectives : resolvedAugCodeSpecDirectives) {
            allDirectives.addAll(resolvedAugCodeDirectives);
            totalDirectiveCount += resolvedAugCodeDirectives.size();
        }
        if (totalDirectiveCount != allDirectives.stream().filter(x -> x != null && !x.trim().isEmpty()).count()) {
            throw new MojoExecutionException("duplicates and/or blanks detected across directives");
        }

        // Validation successful, so begin execution by fetching files inside file sets.
        List<File> baseDirs = new ArrayList<>();
        List<String> relativePaths = new ArrayList<>();
        
        FileSetManager fileSetManager = new FileSetManager();
        for (FileSet fileset : resolvedFileSets) {      
            File baseDir = new File(fileset.getDirectory());
            String[] includedFiles = fileSetManager.getIncludedFiles( fileset );
            for (String includedFile : includedFiles) {
                baseDirs.add(baseDir);
                assert !includedFile.startsWith("/");
                assert !includedFile.startsWith("\\");
                relativePaths.add(includedFile);
            }
        }

        Log logger = task.getLog();
        if (relativePaths.isEmpty()) {
            logger.warn("No files were found");
        }
        else {
            logger.info(String.format("Found %s file(s)", relativePaths.size()));
        }

        PreCodeAugmentationGenericTask genericTask = new PreCodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));        
        genericTask.setPrepFile(resolvedPrepFile);
        genericTask.setRelativePaths(relativePaths);
        genericTask.setBaseDirs(baseDirs);
        genericTask.setGenCodeStartDirectives(resolvedGenCodeStartDirectives);
        genericTask.setGenCodeEndDirectives(resolvedGenCodeEndDirectives);
        genericTask.setEmbeddedStringDirectives(resolvedEmbeddedStringDirectives);
        genericTask.setEmbeddedJsonDirectives(resolvedEmbeddedJsonDirectives);
        genericTask.setSkipCodeStartDirectives(resolvedSkipCodeStartDirectives);
        genericTask.setSkipCodeEndDirectives(resolvedSkipCodeEndDirectives);

        List<AugCodeProcessingSpec> augCodeProcessingSpecs = new ArrayList<>();
        genericTask.setAugCodeProcessingSpecs(augCodeProcessingSpecs);
        for (int i = 0; i < resolvedAugCodeFiles.size(); i++) {
            File resolvedDestFile = resolvedAugCodeFiles.get(i);
            List<String> resolvedAugCodeDirectives = resolvedAugCodeSpecDirectives.get(i);
            AugCodeProcessingSpec augCodeProcessingSpec = new AugCodeProcessingSpec(
                resolvedDestFile, resolvedAugCodeDirectives);
            augCodeProcessingSpecs.add(augCodeProcessingSpec);
        }
        
        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            logger.info("Configuration properties:");
            logger.info("\tencoding: " + genericTask.getCharset());
            logger.info("\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
            logger.info("\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
            logger.info("\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
            logger.info("\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
            logger.info("\tskipCodeStartDirectives: " + genericTask.getSkipCodeStartDirectives());
            logger.info("\tskipCodeEndDirectives: " + genericTask.getSkipCodeEndDirectives());
            
            if (task instanceof PreparationMojo) {
                logger.info("\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getAugCodeProcessingSpecs().size(); i++) {
                    AugCodeProcessingSpec augCodeSpec = genericTask.getAugCodeProcessingSpecs().get(i);
                    logger.info("\taugCodeSpecs[" + i + "].directives: " + augCodeSpec.getDirectives());
                    logger.info("\taugCodeSpecs[" + i + "].destFile: " + augCodeSpec.getDestFile());
                }
            }
            else {
                logger.info("\taugCodeDirectives: " + 
                    genericTask.getAugCodeProcessingSpecs().get(0).getDirectives());
            }

            logger.info("\tfileSets: " + resolvedFileSets);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            logger.info("\tgenericTask.baseDirs: " + new HashSet<>(genericTask.getBaseDirs()));
            logger.info("\tgenericTask.relativePaths: " + genericTask.getRelativePaths());
        }
        
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause());
        }

        // fail build if there were errors.
        if (!genericTask.getAllErrors().isEmpty()) {
            throw TaskUtils.convertToPluginException(genericTask.getAllErrors());
        }
    }
//:SKIP_CODE_END:
}