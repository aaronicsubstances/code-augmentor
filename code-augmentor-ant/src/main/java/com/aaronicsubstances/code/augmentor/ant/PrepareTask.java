package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.AugCodeProcessingSpec;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;
import com.aaronicsubstances.code.augmentor.core.tasks.PreCodeAugmentationGenericTask;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class PrepareTask extends Task {
    private String encoding;
    private boolean verbose;
    private File prepFile;
    private final List<FileSet> srcDirs = new ArrayList<>();
    private final List<String> genCodeStartDirectives = new ArrayList<>();
    private final List<String> genCodeEndDirectives = new ArrayList<>();
    private final List<String> embeddedStringDirectives = new ArrayList<>();
    private final List<String> embeddedJsonDirectives = new ArrayList<>();
    private final List<String> skipCodeStartDirectives = new ArrayList<>();
    private final List<String> skipCodeEndDirectives = new ArrayList<>();
    private final List<String> inlineGenCodeDirectives = new ArrayList<>();
    private final List<String> nestedLevelStartMarkers = new ArrayList<>();
    private final List<String> nestedLevelEndMarkers = new ArrayList<>();
    private final List<AugCodeDirectiveSpec> augCodeSpecs = new ArrayList<>();

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setPrepFile(File prepFile) {
        this.prepFile = prepFile;
    }

    public void addSrcDir(FileSet f) {
        srcDirs.add(f);
    }

    public void addConfiguredGenCodeStartDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        genCodeStartDirectives.add(val);
    }

    public void addConfiguredGenCodeEndDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        genCodeEndDirectives.add(val);
    }

    public void addConfiguredEmbeddedStringDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        embeddedStringDirectives.add(val);
    }

    public void addConfiguredEmbeddedJsonDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        embeddedJsonDirectives.add(val);
    }

    public void addConfiguredSkipCodeStartDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        skipCodeStartDirectives.add(val);
    }

    public void addConfiguredSkipCodeEndDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        skipCodeEndDirectives.add(val);
    }

    public void addConfiguredInlineGenCodeDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        inlineGenCodeDirectives.add(val);
    }

    public void addConfiguredNestedLevelStartMarker(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        nestedLevelStartMarkers.add(val);
    }

    public void addConfiguredNestedLevelEndMarker(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        nestedLevelEndMarkers.add(val);
    }

    public void addAugCodeSpec(AugCodeDirectiveSpec augCodeSpec) {
        augCodeSpecs.add(augCodeSpec);
    }

    public void execute() {
        try {
            List<List<String>> resolvedAugCodeSpecDirectives = new ArrayList<>();
            List<File> resolvedAugCodeFiles = new ArrayList<>();
            for (AugCodeDirectiveSpec augCodeSpec : augCodeSpecs) {
                List<String> directives = null;
                File destFile = null;
                if (augCodeSpec != null) {
                    directives = augCodeSpec.getDirectives();
                    destFile = augCodeSpec.getDestFile();
                }
                resolvedAugCodeSpecDirectives.add(directives);
                resolvedAugCodeFiles.add(destFile);
            }
            completeExecute(this, encoding, verbose, srcDirs, 
                genCodeStartDirectives, genCodeEndDirectives, 
                embeddedStringDirectives, embeddedJsonDirectives, 
                skipCodeStartDirectives, skipCodeEndDirectives, 
                resolvedAugCodeSpecDirectives, resolvedAugCodeFiles, 
                prepFile,
                inlineGenCodeDirectives,
                nestedLevelStartMarkers, nestedLevelEndMarkers);
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BuildException("General error: " + ex, ex);
        }
    }

//:SKIP_CODE_START:
    static void completeExecute(
            Task task,
            String resolvedEncoding, boolean resolvedVerbose, 
            List<FileSet> resolvedFileSets,
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
        // set up defaults
        if (resolvedEncoding == null) {
            resolvedEncoding = "UTF-8";
        }
        if (resolvedGenCodeStartDirectives.isEmpty()) {
            resolvedGenCodeStartDirectives.add("//:GEN_CODE_START:");
        }
        if (resolvedGenCodeEndDirectives.isEmpty()) {
            resolvedGenCodeEndDirectives.add("//:GEN_CODE_END:");
        }
        if (resolvedEmbeddedStringDirectives.isEmpty()) {
            resolvedEmbeddedStringDirectives.add("//:STR:");
        }
        if (resolvedEmbeddedJsonDirectives.isEmpty()) {
            resolvedEmbeddedJsonDirectives.add("//:JSON:");
        }
        if (resolvedSkipCodeStartDirectives.isEmpty()) {
            resolvedSkipCodeStartDirectives.add("//:SKIP_CODE_START:");
        }
        if (resolvedSkipCodeEndDirectives.isEmpty()) {
            resolvedSkipCodeEndDirectives.add("//:SKIP_CODE_END:");
        }
        if (resolvedInlineGenCodeDirectives.isEmpty()) {
            resolvedInlineGenCodeDirectives.add("/*:GEN_CODE:*/");
        }
        if (resolvedNestedLevelStartMarkers.isEmpty()) {
            resolvedNestedLevelStartMarkers.add("{");
        }
        if (resolvedNestedLevelEndMarkers.isEmpty()) {
            resolvedNestedLevelEndMarkers.add("}");
        }
        if (resolvedAugCodeSpecDirectives.isEmpty()) {
            resolvedAugCodeSpecDirectives.add(Arrays.asList("//:AUG_CODE:"));
        }
        if (resolvedAugCodeFiles.isEmpty()) {
            resolvedAugCodeFiles.add(TaskUtils.getDefaultAugCodeFile(task));
        }
        if (resolvedPrepFile == null) {
            resolvedPrepFile = TaskUtils.getDefaultPrepFile(task);
        }
        // validate
        if (resolvedFileSets.isEmpty()) {
            throw new BuildException("at least 1 element is required in srcDirs");
        }
        Charset charset = Charset.forName(resolvedEncoding); // validate encoding.
        for (int i = 0; i < resolvedAugCodeSpecDirectives.size(); i++) {
            List<String> resolvedAugCodeDirectives = resolvedAugCodeSpecDirectives.get(i);
            if (resolvedAugCodeDirectives.isEmpty()) {
                if (task instanceof PrepareTask) {
                    throw new BuildException("at least 1 element is required in augCodeSpecs[" + i + "].directives");
                }
                else {
                    throw new BuildException("at least 1 element is required in augCodeDirectives");
                }
            }
        }
        for (int i = 0; i < resolvedAugCodeFiles.size(); i++) {
            File resolvedAugCodeFile = resolvedAugCodeFiles.get(i);
            if (resolvedAugCodeFile == null) {
                if (task instanceof PrepareTask) {
                    throw new BuildException("invalid null value found at augCodeSpecs[" + i + "]?.destFile");
                }
                else {
                    throw new RuntimeException("unexpected absence of augCodeFile");
                }
            }
        }
        for (int i = 0; i < resolvedFileSets.size(); i++) {
            if (resolvedFileSets.get(i) == null) {
                throw new BuildException("invalid null value found at fileSets[" + i + "]");
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
            throw new BuildException("nulls/blanks detected across directives");
        }
        if (allDirectives.size() != allDirectives.stream().distinct().count()) {
            throw new BuildException("duplicates detected across directives");
        }
        
        // Ensure uniqueness across markers.
        allDirectives.clear();
        addAllIfEnabled(allDirectives, resolvedNestedLevelStartMarkers);
        addAllIfEnabled(allDirectives, resolvedNestedLevelEndMarkers);
        if (allDirectives.stream().anyMatch(x -> x == null || x.trim().isEmpty())) {
            throw new BuildException("nulls/blanks detected across nested level markers");
        }
        if (allDirectives.size() != allDirectives.stream().distinct().count()) {
            throw new BuildException("duplicates detected across nested level markers");
        }

        // Validation successful, so begin execution by fetching files inside file sets.
        List<File> baseDirs = new ArrayList<>();
        List<String> relativePaths = new ArrayList<>();
        
        for (FileSet srcDir : resolvedFileSets) {
            DirectoryScanner ds = srcDir.getDirectoryScanner(task.getProject());
            String[] includedFiles = ds.getIncludedFiles();
            for (String filename : includedFiles) {
                baseDirs.add(ds.getBasedir());
                assert !filename.startsWith("/");
                assert !filename.startsWith("\\");
                relativePaths.add(filename);
            }
        }

        
        if (relativePaths.isEmpty()) {
            task.log("No files were found", Project.MSG_WARN);
        }
        else {
            task.log(String.format("Found %s file(s)", relativePaths.size()));
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
            task.log("Configuration properties:");
            task.log("\tencoding: " + genericTask.getCharset());
            task.log("\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
            task.log("\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
            task.log("\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
            task.log("\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
            task.log("\tskipCodeStartDirectives: " + genericTask.getSkipCodeStartDirectives());
            task.log("\tskipCodeEndDirectives: " + genericTask.getSkipCodeEndDirectives());
            task.log("\tinlineGenCodeDirectives: " + genericTask.getInlineGenCodeDirectives());
            task.log("\tnestedLevelStartMarkers: " + genericTask.getNestedLevelStartMarkers());
            task.log("\tnestedLevelEndMarkers: " + genericTask.getNestedLevelEndMarkers());
            
            if (task instanceof PrepareTask) {
                task.log("\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getAugCodeProcessingSpecs().size(); i++) {
                    AugCodeProcessingSpec augCodeSpec = genericTask.getAugCodeProcessingSpecs().get(i);
                    task.log("\taugCodeSpecs[" + i + "].directives: " + augCodeSpec.getDirectives());
                    task.log("\taugCodeSpecs[" + i + "].destFile: " + augCodeSpec.getDestFile());
                }
            }
            else {
                task.log("\taugCodeDirectives: " + 
                    genericTask.getAugCodeProcessingSpecs().get(0).getDirectives());
            }

            task.log("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            task.log("\tgenericTask.baseDirs: " + new HashSet<>(genericTask.getBaseDirs()));
            // ant's FileSet.toString() prints relative paths of its files
            task.log("\tsrcDirs: " + resolvedFileSets);
        }
        
        try {
            genericTask.execute();
        }
        catch (GenericTaskException ex) {
            throw new BuildException(ex.getMessage(), ex.getCause());
        }

        // fail build if there were errors.
        if (!genericTask.getAllErrors().isEmpty()) {
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                genericTask.getAllErrors(), false, null, null);
            throw new BuildException(allExMsg);
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
}