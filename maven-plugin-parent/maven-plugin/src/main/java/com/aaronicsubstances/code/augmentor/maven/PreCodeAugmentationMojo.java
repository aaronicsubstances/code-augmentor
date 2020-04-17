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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

/**
 * Prepares for code generation.
 *
 */
@Mojo( name = "prepare")
public class PreCodeAugmentationMojo extends AbstractPluginMojo {

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
}