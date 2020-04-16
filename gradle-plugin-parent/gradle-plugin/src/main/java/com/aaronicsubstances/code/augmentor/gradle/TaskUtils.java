package com.aaronicsubstances.code.augmentor.gradle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;

import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

import groovy.util.GroovyScriptEngine;

public class TaskUtils {
    private static final List<String> STACKTRACE_SANITIZER_PREFIXES = Arrays.asList(
        "com.sun.", "sun.", "groovy.lang.", "org.codehaus.groovy."
    );
    private static final String STACKTRACE_SANITIZER_FILTERED_OUT_TEXT = "...";

    public static BiConsumer<GenericTaskLogLevel, Supplier<String>> createLogAppender(Task task) {
        CodeAugmentorPluginExtension ext = (CodeAugmentorPluginExtension) 
            task.getProject().getExtensions().getByName(CodeAugmentorPlugin.EXTENSION_NAME);
        BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            Logger logger = task.getProject().getLogger();
            switch (logLevel) {
                case VERBOSE:
                    if (!ext.getVerbose().get()) {
                        break;
                    }
                    else {
                        // fall through.
                    }
                case INFO:
                    if (logger.isInfoEnabled()) {
                        logger.info(msgFunc.get());
                    }
                    break;
                case WARN:
                    if (logger.isWarnEnabled()) {
                        logger.warn(msgFunc.get());
                    }
                    break;
            }
        };
        return logAppender;
    }

    public static GradleException convertToGradleException(List<Exception> allErrors) {
        StringBuilder allExMsg = new StringBuilder();
        allExMsg.append(allErrors.size()).append(" parse error(s) found.\n");
        for (Exception ex : allErrors) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause == ex) {
                    allExMsg.append(cause.getMessage());
                }
                else {
                    allExMsg.append(" Caused by: ").append(cause);
                }
                allExMsg.append("\n");
                
                List<StackTraceElement> stackTrace = sanitizeStackTrace(cause);
                for (StackTraceElement elem : stackTrace) {
                    allExMsg.append("\t");
                    if (elem == null) {
                        // filtered
                        allExMsg.append(STACKTRACE_SANITIZER_FILTERED_OUT_TEXT);
                    }
                    else {
                        allExMsg.append("at ").append(elem);
                    }
                    allExMsg.append("\n");
                }
                cause = cause.getCause();
            }
        }
        return new GradleException(allExMsg.toString());
    }

    private static List<StackTraceElement> sanitizeStackTrace(Throwable t) {        
        // Only show stack trace if it is about GroovyScriptEngine
        // Also skip irrelevant stack trace elements from Groovy internals.
        List<StackTraceElement> filtered = new ArrayList<>();        
        StackTraceElement[] stackTrace = t.getStackTrace();
        if (stackTrace == null) {
            return filtered;
        }
        Optional<StackTraceElement> stackTraceIndex = Arrays.stream(stackTrace)
            .filter(elem -> GroovyScriptEngine.class.getName().equals(elem.getClassName()))
            .findAny();
        if (!stackTraceIndex.isPresent()) {
            return filtered;
        }
        for (StackTraceElement elem : stackTrace) {
            if (GroovyScriptEngine.class.getName().equals(elem.getClassName())) {
                break;
            }
            if (elem.getClassName() != null) {
                Optional<String> skipElem = STACKTRACE_SANITIZER_PREFIXES.stream()
                    .filter(x -> elem.getClassName().startsWith(x))
                    .findAny();
                if (skipElem.isPresent()) {
                    if (filtered.isEmpty() || filtered.get(filtered.size() - 1) != null) {
                        filtered.add(null);
                    }
                    continue;
                }
            }
            filtered.add(elem);
        }
        // Remove trailing null unless it will lead to emptiness
        if (!filtered.isEmpty() && filtered.get(filtered.size() - 1) == null) {
            if (filtered.size() > 1) {
                filtered.remove(filtered.size() - 1);
            }
        }
        return filtered;
    }
}