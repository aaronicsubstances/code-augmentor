package com.aaronicsubstances.code.augmentor.core.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GenericTaskException extends RuntimeException {
    private static final long serialVersionUID = 5939044801925095816L;

    public static GenericTaskException create(Throwable cause, String message, 
            String srcPath, int srcLineNumber, String srcFileSnippet) {
        StringBuilder fullMessage = new StringBuilder();
        if (srcPath != null) {
            fullMessage.append("in ").append(srcPath);
        }
        if (srcLineNumber > 0) {
            if (fullMessage.length() > 0) {
                fullMessage.append(" ");
            }
            fullMessage.append("at line ").append(srcLineNumber);
        }
        if (fullMessage.length() > 0) {
            fullMessage.append(": ");   
        }
        fullMessage.append(message);
        if (srcFileSnippet != null) {
            fullMessage.append("\n\n").append(srcFileSnippet);
        }
        return new GenericTaskException(srcLineNumber, fullMessage.toString(), cause);
    }

    private final int lineNumber;

    public GenericTaskException(int lineNumber, String message) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public GenericTaskException(int lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }

	public int getLineNumber() {
		return lineNumber;
    }
    
    private static final String GroovyScriptEngine_CLASS_NAME = "groovy.util.GroovyScriptEngine";
    private static final List<String> STACKTRACE_SANITIZER_PREFIXES = Arrays.asList(
        "com.sun.", "sun.", "groovy.lang.", "org.codehaus.groovy."
    );
    private static final String STACKTRACE_SANITIZER_FILTERED_OUT_TEXT = "...";

    public static String toExceptionMessageWithGroovyConsideration(
            List<Exception> allErrors) {
        StringBuilder allExMsg = new StringBuilder();
        allExMsg.append(allErrors.size()).append(" error(s) found.\n");
        for (Exception ex : allErrors) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause == ex) {
                    allExMsg.append(cause.getMessage());
                }
                else {
                    allExMsg.append("Caused by: ").append(cause);
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
                        allExMsg.append("--> ").append(elem);
                    }
                    allExMsg.append("\n");
                }
                cause = cause.getCause();
            }
        }
        for (int i = 0; i < 40; i++) {
            allExMsg.append("-");
            if (i == 20) {
                allExMsg.append("END OF MESSAGE");
            }
        }
        allExMsg.append("\n");
        return allExMsg.toString();
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
            .filter(elem -> GroovyScriptEngine_CLASS_NAME.equals(elem.getClassName()))
            .findAny();
        if (!stackTraceIndex.isPresent()) {
            return filtered;
        }
        for (StackTraceElement elem : stackTrace) {
            if (GroovyScriptEngine_CLASS_NAME.equals(elem.getClassName())) {
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