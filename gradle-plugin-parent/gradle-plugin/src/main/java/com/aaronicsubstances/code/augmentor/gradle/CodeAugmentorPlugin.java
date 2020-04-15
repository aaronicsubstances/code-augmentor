package com.aaronicsubstances.code.augmentor.gradle;

import java.util.Arrays;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

public class CodeAugmentorPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "codeAugmentor";

	@Override
    public void apply(Project project) {
        CodeAugmentorPluginExtension extension = project.getExtensions().create(
            EXTENSION_NAME, CodeAugmentorPluginExtension.class, project);
        
        // set defaults for all required values.
        extension.getEncoding().convention("utf-8");

        DirectoryProperty buildDir = project.getLayout().getBuildDirectory();
        String workingDirPrefix = EXTENSION_NAME + "/";
        extension.getPrepFile().convention(buildDir.file(workingDirPrefix + "prepResults.json"));
        extension.getGenCodeStartDirectives().convention(Arrays.asList("//GS:"));
        extension.getGenCodeEndDirectives().convention(Arrays.asList("//:GE"));
        extension.getEmbeddedStringDirectives().convention(Arrays.asList("//ES:"));
        extension.getEmbeddedJsonDirectives().convention(Arrays.asList("//EJS:"));
        
        AugCodeDirectiveSpec defaultAugCodeSpec = new AugCodeDirectiveSpec(project);
        defaultAugCodeSpec.getDestFile().convention(buildDir.file(workingDirPrefix + "augCodes.json"));
        defaultAugCodeSpec.getDirectives().convention(Arrays.asList("//AUG_CODE:"));
        extension.getAugCodeSpecs().convention(Arrays.asList(defaultAugCodeSpec));

        // continue setting defaults for generate task.
        extension.getChangeSetInfoFile().convention(buildDir.file(workingDirPrefix + "changeSet.txt"));
        extension.getDestDir().convention(buildDir.dir(workingDirPrefix + "generated"));
        extension.getGeneratedCodeFiles().convention(Arrays.asList(
            buildDir.file(workingDirPrefix + "genCodes.json")));

        // lastly set defaults for process task.
        extension.getEntryScriptName().convention("main.groovy");

        // add tasks.
        project.getTasks().register("codeAugmentorPrepare", PreCodeAugmentationTask.class, new Action<PreCodeAugmentationTask>() {

            @Override
            public void execute(PreCodeAugmentationTask prepareTask) {
                prepareTask.setDescription("Extracts augmenting code sections for processing");
                prepareTask.setGroup(EXTENSION_NAME);
                prepareTask.getEncoding().set(extension.getEncoding());
                prepareTask.getFileSets().set(extension.getFileSets());
                prepareTask.getPrepFile().set(extension.getPrepFile());
                prepareTask.getAugCodeSpecs().set(extension.getAugCodeSpecs());
                prepareTask.getGenCodeStartDirectives().set(extension.getGenCodeStartDirectives());
                prepareTask.getGenCodeEndDirectives().set(extension.getGenCodeEndDirectives());
                prepareTask.getEmbeddedStringDirectives().set(extension.getEmbeddedStringDirectives());
                prepareTask.getEmbeddedJsonDirectives().set(extension.getEmbeddedJsonDirectives());
                prepareTask.getEnableScanDirectives().set(extension.getEnableScanDirectives());
                prepareTask.getDisableScanDirectives().set(extension.getDisableScanDirectives());
            }
        });
        project.getTasks().register("codeAugmentorProcess", ProcessCodeTask.class, new Action<ProcessCodeTask>() {

            @Override
            public void execute(ProcessCodeTask processTask) {
                processTask.setDescription("Determines generated code per each extracted augmenting code");
                processTask.setGroup(EXTENSION_NAME);
                processTask.getScriptsDir().set(extension.getScriptsDir());
                processTask.getEntryScriptName().set(extension.getEntryScriptName());
            }
        });
        project.getTasks().register("codeAugmentorGenerate", CodeAugmentationTask.class, new Action<CodeAugmentationTask>() {

            @Override
            public void execute(CodeAugmentationTask generateTask) {
                generateTask.setDescription("Generates for each outdated source file one with generated code section updated");
                generateTask.setGroup(EXTENSION_NAME);
                generateTask.getEncoding().set(extension.getEncoding());
                generateTask.getPrepFile().set(extension.getPrepFile());
                generateTask.getGeneratedCodeFiles().set(extension.getGeneratedCodeFiles());
                generateTask.getDestDir().set(extension.getDestDir());
                generateTask.getChangeSetInfoFile().set(extension.getChangeSetInfoFile());
            }
        });
    }
}