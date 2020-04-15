package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.AugCodeProcessingSpec;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;
import com.aaronicsubstances.code.augmentor.core.util.ParserException;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Prepares for code generation.
 *
 */
public class PreCodeAugmentationTask extends DefaultTask {
    private final Property<String> encoding;
    private final ListProperty<ConfigurableFileTree> fileSets;
    private final Property<Object> prepFile;
    private final ListProperty<AugCodeDirectiveSpec> augCodeSpecs;
    private final ListProperty<String> genCodeStartDirectives;
    private final ListProperty<String> genCodeEndDirectives;
    private final ListProperty<String> embeddedStringDirectives;
    private final ListProperty<String> embeddedJsonDirectives;
    private final ListProperty<String> enableScanDirectives;
    private final ListProperty<String> disableScanDirectives;
    
    public PreCodeAugmentationTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        encoding = objectFactory.property(String.class);
        fileSets = objectFactory.listProperty(ConfigurableFileTree.class);
        prepFile = objectFactory.property(Object.class);
        augCodeSpecs = objectFactory.listProperty(AugCodeDirectiveSpec.class);
        genCodeStartDirectives = objectFactory.listProperty(String.class);
        genCodeEndDirectives = objectFactory.listProperty(String.class);
        embeddedStringDirectives = objectFactory.listProperty(String.class);
        embeddedJsonDirectives = objectFactory.listProperty(String.class);
        enableScanDirectives = objectFactory.listProperty(String.class);
        disableScanDirectives = objectFactory.listProperty(String.class);
    }

    @TaskAction    
    public void execute() throws Exception {
        try {
            _execute();
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new GradleException("General plugin error: " + ex.getMessage(), ex);
        }
    }

    private void _execute() throws Exception {
        // validate
        if (genCodeStartDirectives.get().isEmpty()) {
            throw new GradleException("at least one element is required in genCodeStartDirectives");
        }
        if (genCodeEndDirectives.get().isEmpty()) {
            throw new GradleException("at least one element is required in genCodeEndDirectives");
        }
        // Ensure uniqueness across directives.
        Set<String> allDirectives = new HashSet<>();
        int totalDirectiveCount = 0;
        allDirectives.addAll(genCodeStartDirectives.get());
        totalDirectiveCount += genCodeStartDirectives.get().size();
        allDirectives.addAll(genCodeEndDirectives.get());
        totalDirectiveCount += genCodeEndDirectives.get().size();
        allDirectives.addAll(embeddedStringDirectives.get());
        totalDirectiveCount += embeddedStringDirectives.get().size();
        allDirectives.addAll(embeddedJsonDirectives.get());
        totalDirectiveCount += embeddedJsonDirectives.get().size();
        allDirectives.addAll(enableScanDirectives.get());
        totalDirectiveCount += enableScanDirectives.get().size();
        allDirectives.addAll(disableScanDirectives.get());
        totalDirectiveCount += disableScanDirectives.get().size();

        if (augCodeSpecs.get().isEmpty()) {
            throw new GradleException("at least 1 element is required in augCodeSpecs");
        }
        for (int i = 0; i < augCodeSpecs.get().size(); i++) {
            AugCodeDirectiveSpec spec = augCodeSpecs.get().get(i);
            if (spec == null) {
                throw new GradleException("augCodeSpecs[" + i + "] is null");
            }
            if (spec.getDirectives().get().isEmpty()) {                
                throw new GradleException("at least one element is required in augCodeSpecs[" + i +
                    "].directives");
            }
            allDirectives.addAll(spec.getDirectives().get());
            totalDirectiveCount += spec.getDirectives().get().size();
        }
        if (totalDirectiveCount != allDirectives.stream().filter(x -> x != null).count()) {
            throw new GradleException("duplicates and/or nulls detected across directives");
        }
        
        Charset charset = Charset.forName(encoding.get());
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
        
        for (ConfigurableFileTree fileTree : fileSets.get()) {
            Set<File> includedFiles = fileTree.getFiles();
            File baseDir = fileTree.getDir();
            String baseDirPath = baseDir.getPath();
            for (File file : includedFiles) {
                baseDirs.add(baseDir);
                assert file.getPath().startsWith(baseDirPath);
                String relativePath = file.getPath().substring(baseDirPath.length() + 1);
                assert !relativePath.startsWith("/");
                assert !relativePath.startsWith("\\");
                relativePaths.add(relativePath);
            }
        }

        if (relativePaths.isEmpty()) {
            getLogger().warn("No files were found");
        }
        else {
            getLogger().info(String.format("Found %s file(s)", relativePaths.size()));
        }

        PreCodeAugmentationGenericTask genericTask = new PreCodeAugmentationGenericTask();
        genericTask.setCharset(charset);
        genericTask.setLogAppender(logAppender);
        if (!prepFile.isPresent()) {
            throw new GradleException("prepFile property must be set");
        }
        File resolvedPrepFile = getProject().file(prepFile);
        genericTask.setPrepFile(resolvedPrepFile);
        genericTask.setRelativePaths(relativePaths);
        genericTask.setBaseDirs(baseDirs);
        genericTask.setGenCodeStartDirectives(genCodeStartDirectives.get());
        genericTask.setGenCodeEndDirectives(genCodeEndDirectives.get());
        genericTask.setEmbeddedStringDirectives(embeddedStringDirectives.get());
        genericTask.setEmbeddedJsonDirectives(embeddedJsonDirectives.get());
        genericTask.setEnableScanDirectives(enableScanDirectives.get());
        genericTask.setDisableScanDirectives(disableScanDirectives.get());

        List<AugCodeProcessingSpec> augCodeProcessingSpecs = new ArrayList<>();
        for (int i = 0; i < augCodeSpecs.get().size(); i++) {
            AugCodeDirectiveSpec spec = augCodeSpecs.get().get(i);
            if (!spec.getDestFile().isPresent()) {
                throw new GradleException("augCodeSpecs[" + i + "].destFile property must be set");
            }
            File resolvedDestFile = getProject().file(spec.getDestFile().get());
            AugCodeProcessingSpec augCodeProcessingSpec =   new AugCodeProcessingSpec(
                resolvedDestFile, spec.getDirectives().get());
            augCodeProcessingSpecs.add(augCodeProcessingSpec);
        }
        genericTask.setAugCodeProcessingSpecs(augCodeProcessingSpecs);
        
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause());
        }

        // fail build if there were errors.
        List<ParserException> allErrors = genericTask.getAllErrors();
        if (!allErrors.isEmpty()) {
            for (ParserException ex : allErrors) {
                getLogger().warn("Parse error " + ex);
            }
            throw new GradleException(allErrors.size() + " parse error(s) found.");
        }
    }

    @Internal
    public Property<String> getEncoding() {
        return encoding;
    }

    @Internal
    public ListProperty<ConfigurableFileTree> getFileSets() {
        return fileSets;
    }

    @Internal
    public Property<Object> getPrepFile() {
        return prepFile;
    }

    @Internal
    public ListProperty<AugCodeDirectiveSpec> getAugCodeSpecs() {
        return augCodeSpecs;
    }

    @Internal
    public ListProperty<String> getGenCodeStartDirectives() {
        return genCodeStartDirectives;
    }

    @Internal
    public ListProperty<String> getGenCodeEndDirectives() {
        return genCodeEndDirectives;
    }

    @Internal
    public ListProperty<String> getEmbeddedStringDirectives() {
        return embeddedStringDirectives;
    }

    @Internal
    public ListProperty<String> getEmbeddedJsonDirectives() {
        return embeddedJsonDirectives;
    }

    @Internal
    public ListProperty<String> getEnableScanDirectives() {
        return enableScanDirectives;
    }

    @Internal
    public ListProperty<String> getDisableScanDirectives() {
        return disableScanDirectives;
    }
}