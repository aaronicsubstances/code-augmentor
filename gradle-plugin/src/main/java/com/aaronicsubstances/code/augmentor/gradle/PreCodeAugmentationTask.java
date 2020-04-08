package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

/**
 * Prepares for code generation.
 *
 */
public class PreCodeAugmentationTask extends DefaultTask {

    private String encoding;

    public List<FileSet> fileSets;

    private File prepFile;

    private AugCodeSuffixSpec[] augCodeSuffixSpecs;

    private List<String> embeddedStringDoubleSlashSuffixes;

    private List<String> genCodeStartSuffixes;

    private List<String> genCodeEndSuffixes;

    @TaskAction    
    public void execute() {
        // Ensure uniqueness across comment suffixes.
        Set<String> allSuffixes = new HashSet<>();
        int totalSuffixCount = 0;
        allSuffixes.addAll(embeddedStringDoubleSlashSuffixes);
        totalSuffixCount += embeddedStringDoubleSlashSuffixes.size();
        allSuffixes.addAll(genCodeStartSuffixes);
        totalSuffixCount += genCodeStartSuffixes.size();
        allSuffixes.addAll(genCodeEndSuffixes);
        totalSuffixCount += genCodeEndSuffixes.size();
        for (AugCodeSuffixSpec spec : augCodeSuffixSpecs) {
            allSuffixes.addAll(spec.getSuffixes());
            totalSuffixCount += spec.getSuffixes().size();
        }
        if (totalSuffixCount != allSuffixes.size()) {
            throw new GradleException("Duplicates detected across comment marker suffixes");
        }
        
        Charset charset = Charset.forName(encoding);
        BiConsumer<Integer, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            switch (logLevel) {
                case PreCodeAugmentationGenericTask.LOG_LEVEL_VERBOSE:
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug(msgFunc.get());
                    }
                    break;
                case PreCodeAugmentationGenericTask.LOG_LEVEL_INFO:
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info(msgFunc.get());
                    }
                    break;
                case PreCodeAugmentationGenericTask.LOG_LEVEL_WARN:
                    if (getLogger().isWarnEnabled()) {
                        getLogger().warn(msgFunc.get());
                    }
                    break;
            }
        }; 

        List<File> baseDirs = new ArrayList<>();
        List<String> relativePaths = new ArrayList<>();
        
        for (FileSet srcdir : fileSets) {
            DirectoryScanner ds = srcdir.getDirectoryScanner(getAnt().getProject());
            String[] includedFiles = ds.getIncludedFiles();
            for (String filename : includedFiles) {
                baseDirs.add(ds.getBasedir());
                relativePaths.add(filename);
            }
        }

        getLogger().info(String.format("Found %s file(s)", relativePaths.size()));

        PreCodeAugmentationGenericTask genericTask = new PreCodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(logAppender);
        genericTask.setPrepFile(prepFile);
        genericTask.setRelativePaths(relativePaths);
        genericTask.setBaseDirs(baseDirs);
        genericTask.setEmbeddedStringDoubleSlashSuffixes(
            embeddedStringDoubleSlashSuffixes);
        genericTask.setGenCodeStartSuffixes(genCodeStartSuffixes);
        genericTask.setGenCodeEndSuffixes(genCodeEndSuffixes);

        List<List<String>> augCodeSuffixes = new ArrayList<>();
        List<File> augCodeDestFiles = new ArrayList<>();
        for (AugCodeSuffixSpec spec : augCodeSuffixSpecs) {
            augCodeSuffixes.add(spec.getSuffixes());
            augCodeDestFiles.add(spec.getDestFile());
        }
        genericTask.setAugCodeSuffixes(augCodeSuffixes);
        genericTask.setAugCodeDestFiles(augCodeDestFiles);
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause());
        }
        catch (Exception ex) {
            throw new GradleException("General plugin error", ex);
        }

        // fail build if there were errors.
        List<ParserException> allErrors = genericTask.getAllErrors();
        if (!allErrors.isEmpty()) {
            for (ParserException ex : allErrors) {
                getLogger().warn(String.format("Parse error in %s %s",
                    new File(ex.getDir(), ex.getFilePath()), ex));
            }
            throw new GradleException(allErrors.size() + " parse error(s) found.");
        }
    }
}