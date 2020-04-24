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

    public static String toExceptionMessageWithScriptConsideration(
            List<Throwable> allErrors, boolean includeStackTraces,
            List<String> stackTraceLimitPrefixes, List<String> stackTraceFilterPrefixes) {
        // set up default prefixes
        if (stackTraceLimitPrefixes == null || stackTraceLimitPrefixes.isEmpty()) {
            stackTraceLimitPrefixes = Arrays.asList("groovy.util.GroovyScriptEngine");
        }
        if (stackTraceFilterPrefixes == null || stackTraceFilterPrefixes.isEmpty()) {
            stackTraceFilterPrefixes = Arrays.asList("com.sun.", "sun.", 
                "groovy.lang.", "org.codehaus.groovy.");
        }
        // try and fetch numerical stack trace limit prefixes as
        // a workaround for experimenting with the stack trace prefixes to use.
        List<Integer> hardLimits = new ArrayList<>();
        for (String stackTraceLimitPrefix : stackTraceLimitPrefixes) {
            try {
                int maxFilteredSize = Integer.parseInt(stackTraceLimitPrefix);
                if (maxFilteredSize >= 0) {
                    hardLimits.add(maxFilteredSize);
                }
            }
            catch (NumberFormatException ignore) {}
        }
        final String STACKTRACE_SANITIZER_FILTERED_OUT_TEXT = "...";

        StringBuilder allExMsg = new StringBuilder();
        allExMsg.append(allErrors.size()).append(" error(s) found.\n");
        for (Throwable ex : allErrors) {
            Throwable cause = ex;
            int causeIndex = 0;
            while (cause != null) {
                if (cause == ex) {
                    allExMsg.append(cause.getMessage());
                }
                else {
                    allExMsg.append("Caused by: ").append(cause);
                }
                allExMsg.append("\n");
                
                if (includeStackTraces) {
                    // Pick out the next hard limit to use.
                    // If we have out of hard limits, just use the
                    // last one available.
                    int hardLimit = -1;
                    if (!hardLimits.isEmpty()) {
                        hardLimit = hardLimits.get(hardLimits.size() - 1);
                        if (causeIndex < hardLimits.size()) {
                            hardLimit = hardLimits.get(causeIndex);
                        }
                    }
                    List<StackTraceElement> stackTrace = sanitizeStackTrace(cause,
                        stackTraceLimitPrefixes, stackTraceFilterPrefixes, hardLimit);
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
                }
                cause = cause.getCause();
                causeIndex++;
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

    private static List<StackTraceElement> sanitizeStackTrace(Throwable t,
            List<String> stackTraceLimitPrefixes, List<String> stackTraceFilterPrefixes,
            int maxFilteredSize) {        
        // Only show stack trace if it includes limit prefixes.
        // Also skip irrelevant stack trace elements using filter prefixes.
        List<StackTraceElement> filtered = new ArrayList<>();        
        StackTraceElement[] stackTrace = t.getStackTrace();
        if (stackTrace == null) {
            return filtered;
        }
        Optional<StackTraceElement> relevanceSearch = Arrays.stream(stackTrace)
            .filter(elem -> elem.getClassName() != null && stackTraceLimitPrefixes.stream()
                .filter(x -> elem.getClassName().startsWith(x))
                .findAny().isPresent())
            .findAny();
        if (!relevanceSearch.isPresent()) {
            if (maxFilteredSize == -1) {
                return filtered;
            }
        }
        for (StackTraceElement elem : stackTrace) {
            if (maxFilteredSize != -1 && filtered.size() >= maxFilteredSize) {
                break;
            }
            Optional<String> elemSearch;
            if (elem.getClassName() != null) {
                elemSearch = stackTraceFilterPrefixes.stream()
                    .filter(x -> elem.getClassName().startsWith(x))
                    .findAny();
                if (elemSearch.isPresent()) {
                    if (filtered.isEmpty() || filtered.get(filtered.size() - 1) != null) {
                        filtered.add(null);
                    }
                    continue;
                }
            }
            filtered.add(elem);
            if (maxFilteredSize == -1) {
                elemSearch = stackTraceLimitPrefixes.stream()
                    .filter(x -> elem.getClassName().startsWith(x))
                    .findAny();
                if (elemSearch.isPresent()) {
                    break;
                }
            }
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