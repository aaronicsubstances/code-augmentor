package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskExtensionFunction;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import groovy.lang.Closure;

public class CodeAugmentorTask extends Task {
    private String encoding;
    private boolean verbose;
    private final List<FileSet> srcDirs = new ArrayList<>();
    private final List<String> genCodeStartDirectives = new ArrayList<>();
    private final List<String> genCodeEndDirectives = new ArrayList<>();
    private final List<String> embeddedStringDirectives = new ArrayList<>();
    private final List<String> embeddedJsonDirectives = new ArrayList<>();
    private final List<String> skipCodeStartDirectives = new ArrayList<>();
    private final List<String> skipCodeEndDirectives = new ArrayList<>();
    private final List<String> augCodeDirectives = new ArrayList<>();
    
    private String stackTraceLimitPrefixes;
    private String stackTraceFilterPrefixes;
    private File groovyScriptDir;
    private String groovyEntryScriptName;

    private File destDir;
    private File changeSetInfoFile;

    private File prepFile;
    private File augCodeFile;
    private File genCodeFile;

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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

    public void addConfiguredAugCodeDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        augCodeDirectives.add(val);
    }

    public void setStackTraceLimitPrefixes(String stackTraceLimitPrefixes) {
        this.stackTraceLimitPrefixes = stackTraceLimitPrefixes;
    }

    public void setStackTraceFilterPrefixes(String stackTraceFilterPrefixes) {
        this.stackTraceFilterPrefixes = stackTraceFilterPrefixes;
    }

    public void setGroovyScriptDir(File groovyScriptDir) {
        this.groovyScriptDir = groovyScriptDir;
    }

    public void setGroovyEntryScriptName(String groovyEntryScriptName) {
        this.groovyEntryScriptName = groovyEntryScriptName;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void setChangeSetInfoFile(File changeSetInfoFile) {
        this.changeSetInfoFile = changeSetInfoFile;
    }

    public void setPrepFile(File prepFile) {
        this.prepFile = prepFile;
    }

    public void setAugCodeFile(File augCodeFile) {
        this.augCodeFile = augCodeFile;
    }

    public void setGenCodeFile(File genCodeFile) {
        this.genCodeFile = genCodeFile;
    }

    public void execute() {
        try {
            // prepare...
            // only add aug code directives if there's something specified by
            // user, so that default can be added later on.
            List<List<String>> resolvedAugCodeDirectives = new ArrayList<>();
            if (!augCodeDirectives.isEmpty()) {
                resolvedAugCodeDirectives.add(augCodeDirectives);
            }
            List<File> augCodeFiles = new ArrayList<>();
            if (augCodeFile != null) {
                augCodeFiles.add(augCodeFile);
            }
            PrepareTask.completeExecute(this, encoding, verbose, srcDirs, 
                genCodeStartDirectives, genCodeEndDirectives, 
                embeddedStringDirectives, embeddedJsonDirectives, 
                skipCodeStartDirectives, skipCodeEndDirectives, 
                resolvedAugCodeDirectives, 
                augCodeFiles,
                prepFile);

            // process...
            List<String> resolvedStackTraceLimitPrefixes = new ArrayList<>();
            if (stackTraceLimitPrefixes != null) {
                resolvedStackTraceLimitPrefixes = Arrays.stream(stackTraceLimitPrefixes.split(",", -1))
                    .map(x -> x.trim())
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
            }
            List<String> resolvedStackTraceFilterPrefixes = new ArrayList<>();
            if (stackTraceFilterPrefixes != null) {                
                resolvedStackTraceFilterPrefixes = Arrays.stream(stackTraceFilterPrefixes.split(",", -1))
                    .map(x -> x.trim())
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
            }
            
            GenericTaskExtensionFunction resolvedScriptEvalFunction = null;
            Closure<?> evalClosure = (Closure<?>)getProject().getReference("scriptEvalFunction");
            if (evalClosure != null) {
                resolvedScriptEvalFunction = args -> {
                    return evalClosure.call(Arrays.asList(args));
                };
            }
            
            ProcessTask.completeExecute(this, verbose, 0, 0, augCodeFile, genCodeFile, 
            resolvedScriptEvalFunction, resolvedStackTraceLimitPrefixes, 
                resolvedStackTraceFilterPrefixes, groovyScriptDir, 
                groovyEntryScriptName);

            // complete.
            List<File> genCodeFiles = new ArrayList<>();
            if (genCodeFile != null) {
                genCodeFiles.add(genCodeFile);
            }
            CompletionTask.completeExecute(this, encoding, verbose, prepFile,
                genCodeFiles, destDir, changeSetInfoFile);
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BuildException("General error: " + ex, ex);
        }
    }
}