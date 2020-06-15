package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.tasks.AugCodeProcessingSpec;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;
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
public class PreparationTask extends DefaultTask {
    private final Property<Boolean> verbose;
    private final Property<String> encoding;
    private final ListProperty<ConfigurableFileTree> fileSets;
    private final Property<Object> prepFile;
    private final ListProperty<AugCodeDirectiveSpec> augCodeSpecs;
    private final ListProperty<String> genCodeStartDirectives;
    private final ListProperty<String> genCodeEndDirectives;
    private final ListProperty<String> embeddedStringDirectives;
    private final ListProperty<String> embeddedJsonDirectives;
    private final ListProperty<String> skipCodeStartDirectives;
    private final ListProperty<String> skipCodeEndDirectives;
    private final ListProperty<String> inlineGenCodeDirectives;
    private final ListProperty<String> nestedLevelStartMarkers;
    private final ListProperty<String> nestedLevelEndMarkers;
    
    public PreparationTask() {
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
        skipCodeStartDirectives = objectFactory.listProperty(String.class);
        skipCodeEndDirectives = objectFactory.listProperty(String.class);
        inlineGenCodeDirectives = objectFactory.listProperty(String.class);
        nestedLevelStartMarkers = objectFactory.listProperty(String.class);
        nestedLevelEndMarkers = objectFactory.listProperty(String.class);
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
            List<String> resolvedSkipCodeStartDirectives = skipCodeStartDirectives.get();
            List<String> resolvedSkipCodeEndDirectives = skipCodeEndDirectives.get();
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
            List<String> resolvedInlineGenCodeDirectives = inlineGenCodeDirectives.get();
            List<String> resolvedNestedLevelStartMarkers = nestedLevelStartMarkers.get();
            List<String> resolvedNestedLevelEndMarkers = nestedLevelEndMarkers.get();
            completeExecute(this, resolvedEncoding, resolvedVerbose,
                resolvedFileSets, resolvedGenCodeStartDirectives,
                resolvedGenCodeEndDirectives, resolvedEmbeddedStringDirectives,
                resolvedEmbeddedJsonDirectives, resolvedSkipCodeStartDirectives,
                resolvedSkipCodeEndDirectives, resolvedAugCodeSpecDirectives,
                resolvedAugCodeFiles, resolvedPrepFile,
                resolvedInlineGenCodeDirectives,
                resolvedNestedLevelStartMarkers, resolvedNestedLevelEndMarkers);
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new GradleException("General plugin error: " + ex, ex);
        }
    }

//:SKIP_CODE_START:
    static void completeExecute(
            DefaultTask task,
            String resolvedEncoding, boolean resolvedVerbose, 
            List<ConfigurableFileTree> resolvedFileSets,
            List<String> resolvedGenCodeStartDirectives,
            List<String> resolvedGenCodeEndDirectives,
            List<String> resolvedEmbeddedStringDirectives,
            List<String> resolvedEmbeddedJsonDirectives,
            List<String> resolvedSkipCodeStartDirectives,
            List<String> resolvedSkipCodeEndDirectives,
            List<List<String>> resolvedAugCodeSpecDirectives,
            List<File> resolvedAugCodeFiles,
            File resolvedPrepFile,
            List<String> resolvedInlineGenCodeDirectives,
            List<String> resolvedNestedLevelStartMarkers, 
            List<String> resolvedNestedLevelEndMarkers) throws Exception {
        
        if (resolvedGenCodeStartDirectives.isEmpty()) {
            throw new GradleException("at least 1 element is required in genCodeStartDirectives");
        }
        if (resolvedGenCodeEndDirectives.isEmpty()) {
            throw new GradleException("at least 1 element is required in genCodeEndDirectives");
        }
        if (resolvedAugCodeSpecDirectives.isEmpty()) {
            if (task instanceof PreparationTask) {
                throw new GradleException("at least 1 element is required in augCodeSpecs");
            }
            else {
                throw new RuntimeException("unexpected absence of augCodeDirectives");
            }
        }
        // validate
        if (resolvedFileSets.isEmpty()) {
            throw new GradleException("at least 1 element is required in fileSets");
        }
        Charset charset = Charset.forName(resolvedEncoding); // validate encoding.
        for (int i = 0; i < resolvedAugCodeSpecDirectives.size(); i++) {
            List<String> resolvedAugCodeDirectives = resolvedAugCodeSpecDirectives.get(i);
            if (resolvedAugCodeDirectives.isEmpty()) {
                if (task instanceof PreparationTask) {
                    throw new GradleException("at least 1 element is required in augCodeSpecs[" + i + "].directives");
                }
                else {
                    throw new GradleException("at least 1 element is required in augCodeDirectives");
                }
            }
        }
        for (int i = 0; i < resolvedAugCodeFiles.size(); i++) {
            File resolvedAugCodeFile = resolvedAugCodeFiles.get(i);
            if (resolvedAugCodeFile == null) {
                if (task instanceof PreparationTask) {
                    throw new GradleException("invalid null value found at augCodeSpecs[" + i + "]?.destFile");
                }
                else {
                    throw new RuntimeException("unexpected absence of augCodeFile");
                }
            }
        }
        for (int i = 0; i < resolvedFileSets.size(); i++) {
            if (resolvedFileSets.get(i) == null) {
                throw new GradleException("invalid null value found at fileSets[" + i + "]");
            }
        }

        // Ensure uniqueness across directives.
        List<String> allDirectives = new ArrayList<>();
        addAllIfEnabled(allDirectives, resolvedGenCodeStartDirectives);
        addAllIfEnabled(allDirectives, resolvedGenCodeEndDirectives);
        addAllIfEnabled(allDirectives, resolvedEmbeddedStringDirectives);
        addAllIfEnabled(allDirectives, resolvedEmbeddedJsonDirectives);
        addAllIfEnabled(allDirectives, resolvedSkipCodeStartDirectives);
        addAllIfEnabled(allDirectives, resolvedSkipCodeEndDirectives);
        addAllIfEnabled(allDirectives, resolvedInlineGenCodeDirectives);
        
        for (List<String> resolvedAugCodeDirectives : resolvedAugCodeSpecDirectives) {
            addAllIfEnabled(allDirectives, resolvedAugCodeDirectives);
        }
        if (allDirectives.stream().anyMatch(x -> x == null || x.trim().isEmpty())) {
            throw new GradleException("nulls/blanks detected across directives");
        }
        if (allDirectives.size() != allDirectives.stream().distinct().count()) {
            throw new GradleException("duplicates detected across directives");
        }
        
        // Ensure uniqueness across markers.
        allDirectives.clear();
        addAllIfEnabled(allDirectives, resolvedNestedLevelStartMarkers);
        addAllIfEnabled(allDirectives, resolvedNestedLevelEndMarkers);
        if (allDirectives.stream().anyMatch(x -> x == null || x.trim().isEmpty())) {
            throw new GradleException("nulls/blanks detected across nested level markers");
        }
        if (allDirectives.size() != allDirectives.stream().distinct().count()) {
            throw new GradleException("duplicates detected across nested level markers");
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
        genericTask.setSkipCodeStartDirectives(resolvedSkipCodeStartDirectives);
        genericTask.setSkipCodeEndDirectives(resolvedSkipCodeEndDirectives);
        genericTask.setInlineGenCodeDirectives(resolvedInlineGenCodeDirectives);
        genericTask.setNestedLevelStartMarkers(resolvedNestedLevelStartMarkers);
        genericTask.setNestedLevelEndMarkers(resolvedNestedLevelEndMarkers);

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
            logger.info("\tinlineGenCodeDirectives: " + genericTask.getInlineGenCodeDirectives());
            logger.info("\tnestedLevelStartMarkers: " + genericTask.getNestedLevelStartMarkers());
            logger.info("\tnestedLevelEndMarkers: " + genericTask.getNestedLevelEndMarkers());
            
            if (task instanceof PreparationTask) {
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
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                genericTask.getAllErrors(), false, null, null);
            throw new GradleException(allExMsg);
        }
    }
    
    private static void addAllIfEnabled(List<String> allDirectives, List<String> particularDirectives) {
        // When exactly one blank is specified for a set of directives or markers,
        // interpret as explicit intention to specify a blank, and hence no use for that directive. 
        if (particularDirectives.size() == 1) {
            String loneDirective = particularDirectives.get(0);
            if (loneDirective == null || loneDirective.trim().isEmpty()) {
                return;
            }
        }
        allDirectives.addAll(particularDirectives);
    }
//:SKIP_CODE_END:

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
    public ListProperty<String> getSkipCodeStartDirectives() {
        return skipCodeStartDirectives;
    }

    @Internal
    public ListProperty<String> getSkipCodeEndDirectives() {
        return skipCodeEndDirectives;
    }

    @Internal
    public ListProperty<String> getInlineGenCodeDirectives() {
        return inlineGenCodeDirectives;
    }

    @Internal
    public ListProperty<String> getNestedLevelStartMarkers() {
        return nestedLevelStartMarkers;
    }

    @Internal
    public ListProperty<String> getNestedLevelEndMarkers() {
        return nestedLevelEndMarkers;
    }
}