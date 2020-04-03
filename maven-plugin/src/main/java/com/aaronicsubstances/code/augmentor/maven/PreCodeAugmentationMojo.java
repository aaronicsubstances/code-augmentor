package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;

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
    private AugCodeSuffixSpec[] augCodeSuffixSpecs;

    @Parameter( required=true )
    private String[] embeddedStringDoubleSlashSuffixes;

    @Parameter( required=true )
    private String[] genCodeStartSuffixes;

    @Parameter( required=true )
    private String[] genCodeEndSuffixes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
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

        PreCodeAugmentationGenericTask genericTask = new PreCodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(logAppender);
        genericTask.setPrepFile(prepFile);
        genericTask.setRelativePaths(relativePaths);
        genericTask.setBaseDirs(baseDirs);
        genericTask.setEmbeddedStringDoubleSlashSuffixes(
            Arrays.asList(embeddedStringDoubleSlashSuffixes));
        genericTask.setGenCodeStartSuffixes(Arrays.asList(genCodeStartSuffixes));
        genericTask.setGenCodeEndSuffixes(Arrays.asList(genCodeEndSuffixes));

        List<List<String>> augCodeSuffixes = new ArrayList<>();
        List<File> augCodeDestFiles = new ArrayList<>();
        for (AugCodeSuffixSpec spec : augCodeSuffixSpecs) {
            augCodeSuffixes.add(Arrays.asList(spec.getSuffixes()));
            augCodeDestFiles.add(spec.getDestFile());
        }
        genericTask.setAugCodeSuffixes(augCodeSuffixes);
        genericTask.setAugCodeDestFiles(augCodeDestFiles);
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
                getLog().warn(String.format("Parse error in %s %s",
                    new File(ex.getDir(), ex.getFilePath()), ex));
            }
            throw new MojoExecutionException(allErrors.size() + " parse error(s) found.");
        }
    }
}