package com.aaronicsubstances.code.augmentor.gradle;

import java.util.Arrays;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public class CodeAugmentorPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "codeAugmentor";

	@Override
    public void apply(Project project) {
        CodeAugmentorPluginExtension extension = project.getExtensions().create(
            EXTENSION_NAME, CodeAugmentorPluginExtension.class, project);
        
        // set defaults for common required values.
        extension.getEncoding().convention("utf-8");
        // since info logging is only seen when --info command line arg is used,
        // we make verbose true by default. The assumption is that human user
        // is using --info to view as much details as possible. 
        extension.getVerbose().convention(true);

        // set defaults for prepare task
        DirectoryProperty buildDir = project.getLayout().getBuildDirectory();
        String workingDirPrefix = EXTENSION_NAME + "/";
        Provider<RegularFile> defaultPrepFile = buildDir.file(workingDirPrefix + "prepResults.json");
        Provider<RegularFile> defaultAugCodeFile = buildDir.file(workingDirPrefix + "augCodes.json");
        Provider<RegularFile> defaultGenCodeFile = buildDir.file(workingDirPrefix + "genCodes.json");
        List<String> defaultAugCodeDirectives = Arrays.asList("//:AUG_CODE:");
        extension.getPrepFile().convention(defaultPrepFile);
        extension.getGenCodeStartDirectives().convention(Arrays.asList("//:GEN_CODE_START:"));
        extension.getGenCodeEndDirectives().convention(Arrays.asList("//:GEN_CODE_END:"));
        extension.getEmbeddedStringDirectives().convention(Arrays.asList("//:STR:"));
        extension.getEmbeddedJsonDirectives().convention(Arrays.asList("//:JSON:"));
        extension.getSkipCodeStartDirectives().convention(Arrays.asList("//:SKIP_CODE_START:"));
        extension.getSkipCodeEndDirectives().convention(Arrays.asList("//:SKIP_CODE_END:"));
        extension.getAugCodeDirectives().convention(defaultAugCodeDirectives);
        
        AugCodeDirectiveSpec defaultAugCodeSpec = new AugCodeDirectiveSpec(project);
        defaultAugCodeSpec.getDestFile().convention(defaultAugCodeFile);
        defaultAugCodeSpec.getDirectives().convention(defaultAugCodeDirectives);
        extension.getAugCodeSpecs().convention(Arrays.asList(defaultAugCodeSpec));

        extension.getInlineGenCodeDirectives().convention(Arrays.asList("/*:GEN_CODE:*/"));
        extension.getNestedLevelStartMarkers().convention(Arrays.asList("{"));
        extension.getNestedLevelEndMarkers().convention(Arrays.asList("}"));

        // set defaults for process task.
        extension.getGroovyEntryScriptName().convention("main.groovy");
        extension.getAugCodeSpecIndex().convention(0);
        extension.getGenCodeFileIndex().convention(0);

        // sets defaults for complete task.
        extension.getChangeSetInfoFile().convention(buildDir.file(workingDirPrefix + "changeSet.txt"));
        extension.getDestDir().convention(buildDir.dir(workingDirPrefix + "generated"));
        extension.getGeneratedCodeFiles().convention(Arrays.asList(defaultGenCodeFile));
        extension.getFailOnChanges().convention(true);

        // add tasks.
        project.getTasks().register("codeAugmentorPrepare", PreparationTask.class, new Action<PreparationTask>() {

            @Override
            public void execute(PreparationTask prepareTask) {
                prepareTask.setDescription("Extracts augmenting code sections for processing");
                prepareTask.setGroup(EXTENSION_NAME);
                prepareTask.getVerbose().set(extension.getVerbose());
                prepareTask.getEncoding().set(extension.getEncoding());
                prepareTask.getFileSets().set(extension.getFileSets());
                prepareTask.getPrepFile().set(extension.getPrepFile());
                prepareTask.getAugCodeSpecs().set(extension.getAugCodeSpecs());
                prepareTask.getGenCodeStartDirectives().set(extension.getGenCodeStartDirectives());
                prepareTask.getGenCodeEndDirectives().set(extension.getGenCodeEndDirectives());
                prepareTask.getEmbeddedStringDirectives().set(extension.getEmbeddedStringDirectives());
                prepareTask.getEmbeddedJsonDirectives().set(extension.getEmbeddedJsonDirectives());
                prepareTask.getSkipCodeStartDirectives().set(extension.getSkipCodeStartDirectives());
                prepareTask.getSkipCodeEndDirectives().set(extension.getSkipCodeEndDirectives());
                prepareTask.getInlineGenCodeDirectives().set(extension.getInlineGenCodeDirectives());
                prepareTask.getNestedLevelStartMarkers().set(extension.getNestedLevelStartMarkers());
                prepareTask.getNestedLevelEndMarkers().set(extension.getNestedLevelEndMarkers());
            }
        });
        project.getTasks().register("codeAugmentorProcess", ProcessingTask.class, new Action<ProcessingTask>() {

            @Override
            public void execute(ProcessingTask processTask) {
                processTask.setDescription("Determines generated code per each extracted augmenting code");
                processTask.setGroup(EXTENSION_NAME);
                processTask.getVerbose().set(extension.getVerbose());
                processTask.getGroovyScriptDir().set(extension.getGroovyScriptDir());
                processTask.getGroovyEntryScriptName().set(extension.getGroovyEntryScriptName());
                processTask.getAugCodeSpecs().set(extension.getAugCodeSpecs());
                processTask.getAugCodeSpecIndex().set(extension.getAugCodeSpecIndex());
                processTask.getGeneratedCodeFiles().set(extension.getGeneratedCodeFiles());
                processTask.getGenCodeFileIndex().set(extension.getGenCodeFileIndex());
            }
        });
        project.getTasks().register("codeAugmentorComplete", CompletionTask.class, new Action<CompletionTask>() {

            @Override
            public void execute(CompletionTask generateTask) {
                generateTask.setDescription("Generates for each outdated source file one with generated code section updated");
                generateTask.setGroup(EXTENSION_NAME);
                generateTask.getVerbose().set(extension.getVerbose());
                generateTask.getEncoding().set(extension.getEncoding());
                generateTask.getPrepFile().set(extension.getPrepFile());
                generateTask.getGeneratedCodeFiles().set(extension.getGeneratedCodeFiles());
                generateTask.getDestDir().set(extension.getDestDir());
                generateTask.getChangeSetInfoFile().set(extension.getChangeSetInfoFile());
                generateTask.getFailOnChanges().set(extension.getFailOnChanges());
            }
        });
        project.getTasks().register("codeAugmentorRun", CodeAugmentorTask.class, new Action<CodeAugmentorTask>() {

            @Override
            public void execute(CodeAugmentorTask runTask) {
                runTask.setDescription("Runs code augmentor on source files and generates versions for files which have to be updated");
                runTask.setGroup(EXTENSION_NAME);
                runTask.getVerbose().set(extension.getVerbose());
                runTask.getEncoding().set(extension.getEncoding());
                runTask.getFileSets().set(extension.getFileSets());
                runTask.getAugCodeDirectives().set(extension.getAugCodeDirectives());
                runTask.getGenCodeStartDirectives().set(extension.getGenCodeStartDirectives());
                runTask.getGenCodeEndDirectives().set(extension.getGenCodeEndDirectives());
                runTask.getEmbeddedStringDirectives().set(extension.getEmbeddedStringDirectives());
                runTask.getEmbeddedJsonDirectives().set(extension.getEmbeddedJsonDirectives());
                runTask.getSkipCodeStartDirectives().set(extension.getSkipCodeStartDirectives());
                runTask.getSkipCodeEndDirectives().set(extension.getSkipCodeEndDirectives());
                runTask.getInlineGenCodeDirectives().set(extension.getInlineGenCodeDirectives());
                runTask.getNestedLevelStartMarkers().set(extension.getNestedLevelStartMarkers());
                runTask.getNestedLevelEndMarkers().set(extension.getNestedLevelEndMarkers());
                runTask.getGroovyScriptDir().set(extension.getGroovyScriptDir());
                runTask.getGroovyEntryScriptName().set(extension.getGroovyEntryScriptName());
                runTask.getDestDir().set(extension.getDestDir());
                runTask.getChangeSetInfoFile().set(extension.getChangeSetInfoFile());
                runTask.getFailOnChanges().set(extension.getFailOnChanges());

                // Set these properties as readonly.                
                ((Property<Object>) runTask.getPrepFile()).set(defaultPrepFile);
                ((Property<Object>) runTask.getAugCodeFile()).set(defaultAugCodeFile);
                ((Property<Object>) runTask.getGenCodeFile()).set(defaultGenCodeFile);
            }
        });
    }
}