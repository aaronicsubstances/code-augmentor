package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Houses utility methods for plugins in one place to prevent duplication
 * across plugins.
 */
public class PluginUtils {

    /**
     * Launches an OS process, waits for it to complete (for a maximum of 1 minute),
     * and returns the exit status. Used by plugins for testing.
     * @param workingDir optional current directory the process should run in.
     * @param stdout optional destination file for standard output of process
     * @param stderr optional destination file for standard error of process
     * @param cmdPath required path to executable for process.
     * @param isBatchCmdOnWindows if true, then cmd.exe will be used to
     * run executable at cmdPath on Windows OS. Useful for running .BAT files and 
     * files with other non .EXE extensions which will fail to launch directly on Windows.
     * @param args command line arguments
     * @return exit status of process
     * @throws Exception
     */
    public static int execCommand(File workingDir, File stdout, File stderr, 
            String cmdPath, boolean isBatchCmdOnWindows, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> fullCmd = new ArrayList<>();
        fullCmd.add(cmdPath);
        fullCmd.addAll(Arrays.asList(args));
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            fullCmd.addAll(0, Arrays.asList("cmd", "/c"));
        }
        pb.command(fullCmd);
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        if (stdout != null) {
            pb.redirectOutput(stdout);
        }
        if (stderr != null) {
            pb.redirectError(stderr);
        }
        Process proc = pb.start();
        proc.waitFor(1, TimeUnit.MINUTES);
        return proc.exitValue();
    }

    /**
     * Helper method for converting exception objects to string output. Useful for plugins
     * written or running in scripting language contexts, which can generate a lot of noise 
     * in stack traces, and hide the real underlying exception causes.
     * @param allErrors exception objects to convert in string output.
     * @param includeStackTraces true to include stack traces of errors in string output; false
     * to return only messages of an exception and its descendant inner exceptions, 
     * and skip stack traces entirely.
     * @param stackTraceLimitPrefixes only used when includeStackTraces is true. This list
     * defines how many of the top stack trace elements to show. 
     * <p>
     * For a stack trace to show at all, it must contain a stack trace element whose
     * class name starts with a member of the list (subsequently referred to as limit
     * stack trace element). Then only stack trace elements starting
     * from the top up to and including the limit stack trace element are returned; all stack trace
     * elements after limit stack trace element are discarded. This discarding is one way for
     * addressing the stack trace noise issue.
     * <p>
     * This parameter offers another convenience to script writers to experiment with the proper value
     * to use for it. That convenience is that this parameter can accept strings which are actually 
     * integers. A specified integer determines the size of top stack trace elements to 
     * return. E.g. [ 5 ] means return top 5 stack trace elements. 
     * <p>
     * Multiple integers can be specified, and they are applied consecutively to each inner exception
     * found per each exception object in allErrors. E.g. if [ 5, 4, 3 ] is specified, then for each
     * member of allErrors, 
     * <ol>
     *  <li>that member itself will have its 5 top stack trace elements included in
     * string output.
     *  <li>if it has a nested exception, then that child will have its 4 top stack trace elements
     * included in string output.
     *  <li>if the child in (2) has a child too, then that child (ie grand child of allErrors member)
     * will have its 3 top stack trace elements included in string output.
     *  <li>if the child in (3) has a child exception, then since list is exhausted, the last item of 3
     * will be used: this exception and any inner exceptions will have their top 3 stack trace elements
     * included in string output. 
     * </ol>
     * For each exception object (allErrors member or any of its descendant inner exceptions), 
     * strings take precedence over integers. That is, only when a limiting stack trace element is
     * not found would an attempt be made to apply integer limit.
     * <p>
     * <b>Defaults to [ "groovy.util.GroovyScriptEngine" ]</b>
     * @param stackTraceFilterPrefixes only used when includeStackTraces is true. This list
     * indicates stack trace elements to hide/skip if their class names have any member of the list as
     * its prefix. This parameter is the other way for dealing with the noise issue hinted above.
     * <b>Defaults to [ "com.sun.", "sun.", "groovy.lang.", "org.codehaus.groovy." ]</b>
     * @return string containing messages and included stack traces for allErrors members.
     */
    public static String stringifyPossibleScriptErrors(
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
            if (elem.getClassName() != null && maxFilteredSize == -1) {
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