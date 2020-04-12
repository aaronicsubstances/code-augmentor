package com.aaronicsubstances.code.augmentor.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CodeAugmentorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // set create extension and set defaults.
        CodeAugmentorPluginExtension extension = project.getExtensions().create(
            "code_augmentor", CodeAugmentorPluginExtension.class);
        extension.getEncoding().convention("utf-8");

        // add tasks.
        project.getTasks().register("code_aug_prepare", PreCodeAugmentationTask.class, new Action<PreCodeAugmentationTask>() {

            @Override
            public void execute(PreCodeAugmentationTask prepareTask) {
                prepareTask.getEncoding().set(extension.getEncoding());
                prepareTask.getFileSets().set(extension.getFileSets());
                prepareTask.getPrepFile().set(extension.getPrepFile());
                prepareTask.getAugCodeDirectives().set(extension.getAugCodeDirectives());
                prepareTask.getGenCodeStartDirectives().set(extension.getGenCodeStartDirectives());
                prepareTask.getGenCodeEndDirectives().set(extension.getGenCodeEndDirectives());
                prepareTask.getEmbeddedStringDirectives().set(extension.getEmbeddedStringDirectives());
                prepareTask.getEmbeddedJsonDirectives().set(extension.getEmbeddedJsonDirectives());
                prepareTask.getEnableScanDirectives().set(extension.getEnableScanDirectives());
                prepareTask.getDisableScanDirectives().set(extension.getDisableScanDirectives());
            }
        });
        project.getTasks().register("code_aug_generate", CodeAugmentationTask.class, new Action<CodeAugmentationTask>() {

            @Override
            public void execute(CodeAugmentationTask generateTask) {
                generateTask.getEncoding().set(extension.getEncoding());
                generateTask.getPrepFile().set(extension.getPrepFile());
                generateTask.getGeneratedCodeFiles().set(extension.getGeneratedCodeFiles());
                generateTask.getDestDir().set(extension.getDestDir());
                generateTask.getChangeSetInfoFile().set(extension.getChangeSetInfoFile());
            }
        });
    }
}