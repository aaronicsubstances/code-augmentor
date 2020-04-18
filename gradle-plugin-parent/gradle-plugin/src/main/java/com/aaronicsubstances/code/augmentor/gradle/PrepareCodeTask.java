package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.tasks.AugCodeProcessingSpec;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Prepares for code generation.
 *
 */
public class PrepareCodeTask extends DefaultTask {
    private final Property<Boolean> verbose;
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
    
    public PrepareCodeTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        verbose = objectFactory.property(Boolean.class);
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
    public void execute() throws GradleException {
        try {
            String resolvedEncoding = encoding.get();
            boolean resolvedVerbose = verbose.get(); 
            List<ConfigurableFileTree> resolvedFileSets = fileSets.get();
            List<String> resolvedGenCodeStartDirectives = genCodeStartDirectives.get();
            List<String> resolvedGenCodeEndDirectives = genCodeEndDirectives.get();
            List<String> resolvedEmbeddedStringDirectives = embeddedStringDirectives.get();
            List<String> resolvedEmbeddedJsonDirectives = embeddedJsonDirectives.get();
            List<String> resolvedEnableScanDirectives = enableScanDirectives.get();
            List<String> resolvedDisableScanDirectives = disableScanDirectives.get();
            List<List<String>> resolvedAugCodeSpecDirectives = new ArrayList<>();
            List<File> resolvedAugCodeFiles = new ArrayList<>();
            List<AugCodeDirectiveSpec> resolvedAugCodeSpecs = augCodeSpecs.get();
            for (int i = 0; i < resolvedAugCodeSpecs.size(); i++) {
                List<String> augCodeDirectives = null;
                File resolvedDestFile = null;
                AugCodeDirectiveSpec augCodeSpec = resolvedAugCodeSpecs.get(i);
                if (augCodeSpec != null) {
                    augCodeDirectives = augCodeSpec.getDirectives().get();
                    resolvedDestFile = getProject().file(augCodeSpec.getDestFile());
                }
                resolvedAugCodeSpecDirectives.add(augCodeDirectives);
                resolvedAugCodeFiles.add(resolvedDestFile);
            }
            File resolvedPrepFile = getProject().file(prepFile);
            completeExecute(this, resolvedEncoding, resolvedVerbose,
                resolvedFileSets, resolvedGenCodeStartDirectives,
                resolvedGenCodeEndDirectives, resolvedEmbeddedStringDirectives,
                resolvedEmbeddedJsonDirectives, resolvedEnableScanDirectives,
                resolvedDisableScanDirectives, resolvedAugCodeSpecDirectives,
                resolvedAugCodeFiles, resolvedPrepFile);
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new GradleException("General plugin error: " + ex, ex);
        }
    }
    
    static void completeExecute(
            DefaultTask task,
            String resolvedEncoding, boolean resolvedVerbose, 
            List<ConfigurableFileTree> resolvedFileSets,
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
            throw new GradleException("at least 1 element is required in fileSets");
        }
        Charset charset = Charset.forName(resolvedEncoding); // validate encoding.
        if (resolvedGenCodeStartDirectives.isEmpty()) {
            throw new GradleException("at least 1 element is required in genCodeStartDirectives");
        }
        if (resolvedGenCodeEndDirectives.isEmpty()) {
            throw new GradleException("at least 1 element is required in genCodeEndDirectives");
        }

        if (resolvedAugCodeSpecDirectives.isEmpty()) {
            if (task instanceof PrepareCodeTask) {
                throw new GradleException("at least 1 element is required in augCodeSpecs");
            }
            else {
                throw new RuntimeException("unexpected absence of augCodeDirectives");
            }
        }
        for (int i = 0; i < resolvedAugCodeSpecDirectives.size(); i++) {
            List<String> resolvedAugCodeDirectives = resolvedAugCodeSpecDirectives.get(i);
            if (resolvedAugCodeDirectives.isEmpty()) {
                if (task instanceof PrepareCodeTask) {
                    throw new GradleException("at least 1 element is required in augCodeSpecs[" + i +
                        "].directives");
                }
                else {
                    throw new GradleException("at least 1 element is required in augCodeDirectives");
                }
            }
        }
        for (int i = 0; i < resolvedAugCodeFiles.size(); i++) {
            File resolvedAugCodeFile = resolvedAugCodeFiles.get(i);
            if (resolvedAugCodeFile == null) {
                if (task instanceof PrepareCodeTask) {
                    throw new GradleException("invalid null value found at augCodeSpecs[" + i +
                        "]?.destFile");
                }
                else {
                    throw new RuntimeException("unexpected null for augCodeFile");
                }
            }
        }
        for (int i = 0; i < resolvedFileSets.size(); i++) {
            if (resolvedFileSets.get(i) == null) {
                throw new GradleException("fileSets[" + i + "] is null");
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
        if (totalDirectiveCount != allDirectives.stream().filter(x -> x != null).count()) {
            throw new GradleException("duplicates and/or nulls detected across directives");
        }

        // Validation successful, so begin execution by fetching files inside file sets.
        List<File> baseDirs = new ArrayList<>();
        List<String> relativePaths = new ArrayList<>();
        
        for (ConfigurableFileTree fileTree : resolvedFileSets) {
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

        Logger logger = task.getLogger();
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
            AugCodeProcessingSpec augCodeProcessingSpec =   new AugCodeProcessingSpec(
                resolvedDestFile, resolvedAugCodeDirectives);
            augCodeProcessingSpecs.add(augCodeProcessingSpec);
        }
        
        if (resolvedVerbose) {
            // print task properties - generic task ones, and any ones outside
            logger.info("Configuration properties:");
            logger.info("\tencoding: " + genericTask.getCharset());
            logger.info("\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
            logger.info("\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
            logger.info("\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
            logger.info("\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
            logger.info("\tenableScanDirectives: " + genericTask.getEnableScanDirectives());
            logger.info("\tdisableScanDirectives: " + genericTask.getDisableScanDirectives());
            
            if (task instanceof PrepareCodeTask) {
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
            throw new GradleException(ex.getMessage(), ex.getCause());
        }

        // fail build if there were errors.
        if (!genericTask.getAllErrors().isEmpty()) {
            throw TaskUtils.convertToGradleException(genericTask.getAllErrors());
        }
    }

    @Internal
    public Property<Boolean> getVerbose() {
        return verbose;
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