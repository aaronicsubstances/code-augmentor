package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.AugCodeProcessingSpec;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.util.ParserException;

import org.apache.maven.plugin.AbstractMojo;
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
public class PreCodeAugmentationMojo extends AbstractMojo {

    @Parameter( defaultValue="${project.build.sourceEncoding}", readonly=true, required=true )
    private String encoding;

    @Parameter( required=true )
    public FileSet[] fileSets;

    @Parameter( required=true )
    private File prepFile;

    @Parameter( required=true )
    private AugCodeDirectiveSpec[] augCodeDirectiveSpecs;

    @Parameter( required=true )
    private String[] genCodeStartDirectives;

    @Parameter( required=true )
    private String[] genCodeEndDirectives;

    @Parameter( required=true )
    private String[] embeddedStringDirectives;

    @Parameter( required=true )
    private String[] embeddedJsonDirectives;

    @Parameter
    private String[] enableScanDirectives;

    @Parameter
    private String[] disableScanDirectives;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
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
        if (enableScanDirectives != null) {
            allDirectives.addAll(Arrays.asList(enableScanDirectives));
            totalDirectiveCount += enableScanDirectives.length;
        }
        if (disableScanDirectives != null) {
            allDirectives.addAll(Arrays.asList(disableScanDirectives));
            totalDirectiveCount += disableScanDirectives.length;
        }
        for (AugCodeDirectiveSpec spec : augCodeDirectiveSpecs) {
            allDirectives.addAll(Arrays.asList(spec.getDirectives()));
            totalDirectiveCount += spec.getDirectives().length;
        }
        if (totalDirectiveCount != allDirectives.size()) {
            throw new MojoFailureException("Duplicates detected across directives");
        }
        
        Charset charset = Charset.forName(encoding);
        BiConsumer<Integer, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            switch (logLevel) {
                case PreCodeAugmentationGenericTask.LOG_LEVEL_VERBOSE:
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(msgFunc.get());
                    }
                    break;
                case PreCodeAugmentationGenericTask.LOG_LEVEL_INFO:
                    if (getLog().isInfoEnabled()) {
                        getLog().info(msgFunc.get());
                    }
                    break;
                case PreCodeAugmentationGenericTask.LOG_LEVEL_WARN:
                    if (getLog().isWarnEnabled()) {
                        getLog().warn(msgFunc.get());
                    }
                    break;
            }
        }; 

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
        genericTask.setLogAppender(logAppender);
        genericTask.setPrepFile(prepFile);
        genericTask.setRelativePaths(relativePaths);
        genericTask.setBaseDirs(baseDirs);
        genericTask.setGenCodeStartDirectives(Arrays.asList(genCodeStartDirectives));
        genericTask.setGenCodeEndDirectives(Arrays.asList(genCodeEndDirectives));
        genericTask.setEmbeddedStringDirectives(
            Arrays.asList(embeddedStringDirectives));
        genericTask.setEmbeddedJsonDirectives(
            Arrays.asList(embeddedJsonDirectives));
        if (enableScanDirectives != null) {
            genericTask.setEnableScanDirectives(Arrays.asList(enableScanDirectives));
        }
        if (disableScanDirectives != null) {
            genericTask.setDisableScanDirectives(Arrays.asList(disableScanDirectives));
        }

        List<AugCodeProcessingSpec> augCodeProcessingSpecs = new ArrayList<>();
        for (AugCodeDirectiveSpec spec : augCodeDirectiveSpecs) {
            AugCodeProcessingSpec augCodeProcessingSpec = new AugCodeProcessingSpec(
                spec.getDestFile(),
                Arrays.asList(spec.getDirectives()));
            augCodeProcessingSpecs.add(augCodeProcessingSpec);
        }
        genericTask.setAugCodeProcessingSpecs(augCodeProcessingSpecs);
        
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause());
        }
        catch (Exception ex) {
            throw new MojoFailureException("General plugin error", ex);
        }

        // fail build if there were errors.
        List<ParserException> allErrors = genericTask.getAllErrors();
        if (!allErrors.isEmpty()) {
            for (ParserException ex : allErrors) {
                getLog().warn("Parse error " + ex);
            }
            throw new MojoExecutionException(allErrors.size() + " parse error(s) found.");
        }
    }
}