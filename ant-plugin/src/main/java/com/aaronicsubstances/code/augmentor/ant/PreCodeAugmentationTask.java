package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class PreCodeAugmentationTask extends Task {
    private String encoding;
    private final List<FileSet> srcDirs = new ArrayList<>();
    private File prepFile;
    private final List<CodeGenerationRequestSpecification> requestSpecList = new ArrayList<>();
    private final List<SuffixSpec> embeddedStringDoubleSlashSuffixes = new ArrayList<>();
    private final List<SuffixSpec> genCodeStartSuffixes = new ArrayList<>();
    private final List<SuffixSpec> genCodeEndSuffixes = new ArrayList<>();

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setPrepfile(File f) {
        this.prepFile = f;
    }

    public void addSrc(FileSet d) {
        srcDirs.add(d);
    }

    public void addConfiguredSpec(CodeGenerationRequestSpecification spec) {
        spec.validate();
        requestSpecList.add(spec);
    }

    public void addConfiguredEmbedded_string_dslash_suffix(SuffixSpec suffix) {
        if (suffix.getValue() == null) {            
            throw new BuildException("embedded_string_dslash_suffix[@value] attribute not specified.");
        }
        embeddedStringDoubleSlashSuffixes.add(suffix);
    }

    public void addConfiguredGen_code_start_suffix(SuffixSpec suffix) {
        if (suffix.getValue() == null) {            
            throw new BuildException("gen_code_start_suffix[@value] attribute not specified.");
        }
        genCodeStartSuffixes.add(suffix);
    }

    public void addConfiguredGen_code_end_suffix(SuffixSpec suffix) {
        if (suffix.getValue() == null) {            
            throw new BuildException("gen_code_end_suffix[@value] attribute not specified.");
        }
        genCodeEndSuffixes.add(suffix);
    }

    public void execute() {
        Charset charset = StandardCharsets.UTF_8;
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                throw new BuildException("Invalid value for encoding attribute: " + encoding, ex);
            }
        }
        if (srcDirs.isEmpty()) {
            throw new BuildException("at least 1 nested src element is required");
        }
        if (requestSpecList.isEmpty()) {
            throw new BuildException("at least 1 nested spec element is required");
        }
        if (prepFile == null) {
            throw new BuildException("prepfile attribute is required");
        }
        if (embeddedStringDoubleSlashSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested embedded_string_dslash_suffix element is required");
        }
        if (genCodeStartSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_start_suffix element is required");
        }
        if (genCodeEndSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_end_suffix element is required");
        }

        // Ensure uniqueness across comment suffixes.
        Set<SuffixSpec> allSuffixes = new HashSet<>();
        int totalSuffixCount = 0;
        allSuffixes.addAll(embeddedStringDoubleSlashSuffixes);
        totalSuffixCount += embeddedStringDoubleSlashSuffixes.size();
        allSuffixes.addAll(genCodeStartSuffixes);
        totalSuffixCount += genCodeStartSuffixes.size();
        allSuffixes.addAll(genCodeEndSuffixes);
        totalSuffixCount += genCodeEndSuffixes.size();
        for (CodeGenerationRequestSpecification spec : requestSpecList) {
            allSuffixes.addAll(spec.getAugCodeSuffixes());
            totalSuffixCount += spec.getAugCodeSuffixes().size();
        }
        if (totalSuffixCount != allSuffixes.size()) {
            throw new BuildException("Duplicates detected across comment marker suffixes");
        }
        
        BiConsumer<Integer, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            switch (logLevel) {
                case PreCodeAugmentationGenericTask.LOG_LEVEL_VERBOSE:
                    log(msgFunc.get(), Project.MSG_VERBOSE);
                    break;
                case PreCodeAugmentationGenericTask.LOG_LEVEL_INFO:
                    log(msgFunc.get());
                    break;
                case PreCodeAugmentationGenericTask.LOG_LEVEL_WARN:
                    log(msgFunc.get(), Project.MSG_WARN);
                    break;
            }
        }; 
        
        List<File> baseDirs = new ArrayList<>();
        List<String> relativePaths = new ArrayList<>();
        for (FileSet srcdir : srcDirs) {
            DirectoryScanner ds = srcdir.getDirectoryScanner(getProject());
            String[] includedFiles = ds.getIncludedFiles();
            for (String filename : includedFiles) {
                baseDirs.add(ds.getBasedir());
                relativePaths.add(filename);
            }
        }

        log(String.format("Found %s file(s)", relativePaths.size()));

        PreCodeAugmentationGenericTask genericTask = new PreCodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(logAppender);
        genericTask.setPrepFile(prepFile);
        genericTask.setRelativePaths(relativePaths);
        genericTask.setBaseDirs(baseDirs);
        genericTask.setEmbeddedStringDoubleSlashSuffixes(
            embeddedStringDoubleSlashSuffixes.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));
        genericTask.setGenCodeStartSuffixes(genCodeStartSuffixes.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));
        genericTask.setGenCodeEndSuffixes(genCodeEndSuffixes.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));

        List<List<String>> augCodeSuffixes = new ArrayList<>();
        List<File> augCodeDestFiles = new ArrayList<>();
        for (CodeGenerationRequestSpecification spec : requestSpecList) {
            augCodeSuffixes.add(spec.getAugCodeSuffixes().stream().
                map(x -> x.getValue()).collect(Collectors.toList()));
            augCodeDestFiles.add(spec.getAugCodeDestFile());
        }
        genericTask.setAugCodeSuffixes(augCodeSuffixes);
        genericTask.setAugCodeDestFiles(augCodeDestFiles);
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new BuildException(ex.getMessage(), ex.getCause());
        }
        catch (Exception ex) {
            throw new BuildException("General plugin error", ex);
        }

        // fail build if there were errors.
        List<ParserException> allErrors = genericTask.getAllErrors();
        if (!allErrors.isEmpty()) {
            for (ParserException ex : allErrors) {
                log(String.format("Parse error in %s %s",
                    new File(ex.getDir(), ex.getFilePath()), ex), Project.MSG_WARN);
            }
            throw new BuildException(allErrors.size() + " parse error(s) found.");
        }
    }
}