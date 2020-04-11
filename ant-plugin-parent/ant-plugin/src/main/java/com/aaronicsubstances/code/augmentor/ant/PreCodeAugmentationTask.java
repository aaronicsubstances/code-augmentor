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

import com.aaronicsubstances.code.augmentor.core.tasks.AugCodeProcessingSpec;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.util.ParserException;

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
    private final List<Directive> genCodeStartDirectives = new ArrayList<>();
    private final List<Directive> genCodeEndDirectives = new ArrayList<>();
    private final List<Directive> embeddedStringDirectives = new ArrayList<>();
    private final List<Directive> embeddedJsonDirectives = new ArrayList<>();
    private final List<Directive> enableScanDirectives = new ArrayList<>();
    private final List<Directive> disableScanDirectives = new ArrayList<>();

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

    public void addConfiguredGen_code_start_directive(Directive d) {
        if (d.getValue() == null) {            
            throw new BuildException("gen_code_start_directive[@value] attribute not specified.");
        }
        genCodeStartDirectives.add(d);
    }

    public void addConfiguredGen_code_end_directive(Directive d) {
        if (d.getValue() == null) {            
            throw new BuildException("gen_code_end_directive[@value] attribute not specified.");
        }
        genCodeEndDirectives.add(d);
    }

    public void addConfiguredEmbedded_string_directive(Directive d) {
        if (d.getValue() == null) {            
            throw new BuildException("embedded_string_directive[@value] attribute not specified.");
        }
        embeddedStringDirectives.add(d);
    }

    public void addConfiguredEmbedded_json_directive(Directive d) {
        if (d.getValue() == null) {            
            throw new BuildException("embedded_json_directive[@value] attribute not specified.");
        }
        embeddedJsonDirectives.add(d);
    }

    public void addConfiguredEnable_scan_directive(Directive d) {
        if (d.getValue() == null) {            
            throw new BuildException("enable_scan_directive[@value] attribute not specified.");
        }
        enableScanDirectives.add(d);
    }

    public void addConfiguredDisable_scan_directive(Directive d) {
        if (d.getValue() == null) {            
            throw new BuildException("disable_scan_directive[@value] attribute not specified.");
        }
        disableScanDirectives.add(d);
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

        if (prepFile == null) {
            throw new BuildException("prepfile attribute is required");
        }

        // ensure at least 1 directive in each category, except for 
        // enable/disable scan directives, which are optional.
        if (requestSpecList.isEmpty()) {
            throw new BuildException("at least 1 nested spec element is required");
        }
        if (genCodeStartDirectives.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_start_directive element is required");
        }
        if (genCodeEndDirectives.isEmpty()) {
            throw new BuildException("at least 1 nested gen_code_end_directive element is required");
        }
        if (embeddedStringDirectives.isEmpty()) {
            throw new BuildException("at least 1 nested embedded_string_directive element is required");
        }
        if (embeddedJsonDirectives.isEmpty()) {
            throw new BuildException("at least 1 nested embedded_json_directive element is required");
        }

        // Ensure uniqueness across directives.
        Set<Directive> allDirectives = new HashSet<>();
        int totalDirectiveCount = 0;
        allDirectives.addAll(genCodeStartDirectives);
        totalDirectiveCount += genCodeStartDirectives.size();
        allDirectives.addAll(genCodeEndDirectives);
        totalDirectiveCount += genCodeEndDirectives.size();
        allDirectives.addAll(embeddedStringDirectives);
        totalDirectiveCount += embeddedStringDirectives.size();
        allDirectives.addAll(embeddedJsonDirectives);
        totalDirectiveCount += embeddedJsonDirectives.size();
        allDirectives.addAll(enableScanDirectives);
        totalDirectiveCount += enableScanDirectives.size();
        allDirectives.addAll(disableScanDirectives);
        totalDirectiveCount += disableScanDirectives.size();
        for (CodeGenerationRequestSpecification spec : requestSpecList) {
            allDirectives.addAll(spec.getAugCodeDirectives());
            totalDirectiveCount += spec.getAugCodeDirectives().size();
        }
        if (totalDirectiveCount != allDirectives.size()) {
            throw new BuildException("Duplicates detected across directives");
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
        genericTask.setGenCodeStartDirectives(genCodeStartDirectives.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));
        genericTask.setGenCodeEndDirectives(genCodeEndDirectives.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));
        genericTask.setEmbeddedStringDirectives(
            embeddedStringDirectives.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));
        genericTask.setEmbeddedJsonDirectives(
            embeddedJsonDirectives.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));
        genericTask.setEnableScanDirectives(
            enableScanDirectives.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));
        genericTask.setDisableScanDirectives(
            disableScanDirectives.stream()
            .map(x -> x.getValue())
            .collect(Collectors.toList()));

        List<AugCodeProcessingSpec> augCodeProcessingSpecs = new ArrayList<>();
        for (CodeGenerationRequestSpecification spec : requestSpecList) {
            AugCodeProcessingSpec augCodeProcessingSpec = new AugCodeProcessingSpec(
                spec.getAugCodeDestFile(), 
                spec.getAugCodeDirectives().stream()
                .map(x -> x.getValue()).collect(Collectors.toList()));
            augCodeProcessingSpecs.add(augCodeProcessingSpec);
        }
        genericTask.setAugCodeProcessingSpecs(augCodeProcessingSpecs);
        
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
                log("Parse error " + ex, Project.MSG_WARN);
            }
            throw new BuildException(allErrors.size() + " parse error(s) found.");
        }
    }
}