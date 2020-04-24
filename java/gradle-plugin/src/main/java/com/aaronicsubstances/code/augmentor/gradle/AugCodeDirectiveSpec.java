package com.aaronicsubstances.code.augmentor.gradle;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public class AugCodeDirectiveSpec {
    private final Property<Object> destFile;
    private final ListProperty<String> directives;

    public AugCodeDirectiveSpec(Project project) {
        destFile = project.getObjects().property(Object.class);
        directives = project.getObjects().listProperty(String.class);
    }

    public Property<Object> getDestFile() {
        return destFile;
    }

    public ListProperty<String> getDirectives() {
        return directives;
    }
}