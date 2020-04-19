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

    @Parameter( defaultValue = "//:GS:", required = false )
    private String[] genCodeStartDirectives = new String[0];

    @Parameter( defaultValue = "//:GE:", required = false )
    private String[] genCodeEndDirectives;

    @Parameter( defaultValue = "//:ES:", required = false )
    private String[] embeddedStringDirectives;

    @Parameter( defaultValue = "//:EJS:", required = false )
    private String[] embeddedJsonDirectives = new String[0];

    @Parameter( defaultValue = "//:ENABLE_SCAN:", required = false )
    private String[] enableScanDirectives = new String[0];

    @Parameter( defaultValue = "//:DISABLE_SCAN:", required = false )
    private String[] disableScanDirectives = new String[0];

    @Parameter( required=true )
    public FileSet[] fileSets = new FileSet[0];

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // Set defaults
            AugCodeDirectiveSpec[] augCodeDirectiveSpecs = getAugCodeSpecs();

            // Ensure uniqueness across directives.
            Set<String> allDirectives = new HashSet<>();
            int totalDirectiveCount = 0;
            allDirectives.addAll(Arrays.asList(genCodeStartDirectives));
            totalDirectiveCount += genCodeStartDirectives.length;
            allDirectives.addAll(Arrays.asList(genCodeEndDirectives));
            totalDirectiveCount += genCodeEndDirectives.length;
            allDirectives.addAll(Arrays.asList(embeddedStringDirectives));
            totalDirectiveCount += embeddedStringDirectives.length;
            allDirectives.addAll(Arrays.asList(embeddedJsonDirectives));
            totalDirectiveCount += embeddedJsonDirectives.length;
            allDirectives.addAll(Arrays.asList(enableScanDirectives));
            totalDirectiveCount += enableScanDirectives.length;
            allDirectives.addAll(Arrays.asList(disableScanDirectives));
            totalDirectiveCount += disableScanDirectives.length;
                
            for (AugCodeDirectiveSpec spec : augCodeDirectiveSpecs) {
                allDirectives.addAll(Arrays.asList(spec.getDirectives()));
                totalDirectiveCount += spec.getDirectives().length;
            }
            if (totalDirectiveCount != allDirectives.size()) {
                throw new MojoFailureException("Duplicates detected across directives");
            }
            
            Charset charset = Charset.forName(getEncoding());

            List<File> baseDirs = new ArrayList<>();
            List<String> relativePaths = new ArrayList<>();
            
            FileSetManager fileSetManager = new FileSetManager();
            for (FileSet fileset : fileSets) {      
                File baseDir = new File(fileset.getDirectory());
                String[] includedFiles = fileSetManager.getIncludedFiles( fileset );
                for (String includedFile : includedFiles) {
                    baseDirs.add(baseDir);
                    relativePaths.add(includedFile);
                }
            }

            getLog().info(String.format("Found %s file(s)", relativePaths.size()));

            PreCodeAugmentationGenericTask genericTask = new PreCodeAugmentationGenericTask();
            genericTask.setCharset(charset);
            genericTask.setLogAppender(TaskUtils.createLogAppender(this, isVerbose()));
            genericTask.setPrepFile(getPrepFile());
            genericTask.setRelativePaths(relativePaths);
            genericTask.setBaseDirs(baseDirs);
            genericTask.setGenCodeStartDirectives(Arrays.asList(genCodeStartDirectives));
            genericTask.setGenCodeEndDirectives(Arrays.asList(genCodeEndDirectives));
            genericTask.setEmbeddedStringDirectives(Arrays.asList(embeddedStringDirectives));
            genericTask.setEmbeddedJsonDirectives(Arrays.asList(embeddedJsonDirectives));
            genericTask.setEnableScanDirectives(Arrays.asList(enableScanDirectives));
            genericTask.setDisableScanDirectives(Arrays.asList(disableScanDirectives));

            List<AugCodeProcessingSpec> augCodeProcessingSpecs = new ArrayList<>();
            for (AugCodeDirectiveSpec spec : augCodeDirectiveSpecs) {
                AugCodeProcessingSpec augCodeProcessingSpec = new AugCodeProcessingSpec(
                    spec.getDestFile(), Arrays.asList(spec.getDirectives()));
                augCodeProcessingSpecs.add(augCodeProcessingSpec);
            }
            genericTask.setAugCodeProcessingSpecs(augCodeProcessingSpecs);
            
            if (isVerbose()) {
                // print task properties - generic task ones, and any ones outside
                getLog().info("Configuration properties:");
                getLog().info("\tencoding: " + genericTask.getCharset());
                getLog().info("\tprepFile: " + genericTask.getPrepFile());
                getLog().info("\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
                getLog().info("\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
                getLog().info("\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
                getLog().info("\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
                getLog().info("\tenableScanDirectives: " + genericTask.getEnableScanDirectives());
                getLog().info("\tdisableScanDirectives: " + genericTask.getDisableScanDirectives());
                
                for (int i = 0; i < genericTask.getAugCodeProcessingSpecs().size(); i++) {
                    AugCodeProcessingSpec augCodeSpec = genericTask.getAugCodeProcessingSpecs().get(i);
                    getLog().info("\taugCodeSpecs[" + i + "].directives: " + augCodeSpec.getDirectives());
                    getLog().info("\taugCodeSpecs[" + i + "].destFile: " + augCodeSpec.getDestFile());
                }

                getLog().info("\tfileSets: " + fileSets);
                getLog().info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
                getLog().info("\tgenericTask.baseDirs: " + new HashSet<>(genericTask.getBaseDirs()));
                getLog().info("\tgenericTask.relativePaths: " + genericTask.getRelativePaths());
            }

            try {
                genericTask.execute();
            }
            catch (GenericTaskException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex.getCause());
            }
        
            // fail build if there were errors.
            if (!genericTask.getAllErrors().isEmpty()) {
                throw TaskUtils.convertToMavenException(genericTask.getAllErrors());
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

    static void completeExecute(
            AbstractMojo task,
            String resolvedEncoding, boolean resolvedVerbose, 
            List<FileSet> resolvedFileSets,
            List<String> resolvedGenCodeStartDirectives,
            List<String> resolvedGenCodeEndDirectives,
            List<String> resolvedEmbeddedStringDirectives,
            List<String> resolvedEmbeddedJsonDirectives,
            List<String> resolvedEnableScanDirectives,
            List<String> resolvedDisableScanDirectives,
            List<List<String>> resolvedAugCodeSpecDirectives,
            List<File> resolvedAugCodeFiles,
            File resolvedPrepFile) throws Exception {
        
        // validate
        if (resolvedFileSets.isEmpty()) {
            throw new MojoExecutionException("at least 1 element is required in fileSets");
        }
        Charset charset = Charset.forName(resolvedEncoding); // validate encoding.
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
                    throw new RuntimeException("unexpected null for augCodeFile");
                }
            }
        }
        for (int i = 0; i < resolvedFileSets.size(); i++) {
            if (resolvedFileSets.get(i) == null) {
                throw new MojoExecutionException("fileSets[" + i + "] is null");
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
        allDirectives.addAll(resolvedEnableScanDirectives);
        totalDirectiveCount += resolvedEnableScanDirectives.size();
        allDirectives.addAll(resolvedDisableScanDirectives);
        totalDirectiveCount += resolvedDisableScanDirectives.size();
        
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
        genericTask.setEnableScanDirectives(resolvedEnableScanDirectives);
        genericTask.setDisableScanDirectives(resolvedDisableScanDirectives);

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
            // Print generic task properties
            
            logger.info("Configuration properties:");
            logger.info("\tencoding: " + genericTask.getCharset());
            logger.info("\tprepFile: " + genericTask.getPrepFile());
            logger.info("\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
            logger.info("\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
            logger.info("\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
            logger.info("\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
            logger.info("\tenableScanDirectives: " + genericTask.getEnableScanDirectives());
            logger.info("\tdisableScanDirectives: " + genericTask.getDisableScanDirectives());
            
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
}