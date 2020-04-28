package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AugCodeDirectiveSpec {
    private File destFile;
    private final List<String> directives = new ArrayList<>();

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public void addConfiguredDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        directives.add(val);
    }
}