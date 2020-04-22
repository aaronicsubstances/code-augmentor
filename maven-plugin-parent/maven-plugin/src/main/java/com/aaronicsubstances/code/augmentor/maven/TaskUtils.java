package com.aaronicsubstances.code.augmentor.maven;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class TaskUtils {
    
    public static BiConsumer<GenericTaskLogLevel, Supplier<String>> createLogAppender(
            AbstractMojo mojo, boolean verbose) {
        BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            Log logger = mojo.getLog();
            switch (logLevel) {
                case VERBOSE:
                    if (!verbose) {
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

    public static MojoExecutionException convertToPluginException(List<Throwable> allErrors) {
        return convertToPluginException(allErrors, false, null, null);
    }

    public static MojoExecutionException convertToPluginException(List<Throwable> allErrors,
            boolean includeStackTraces,
            List<String> stackTraceLimitPrefixes, List<String> stackTraceFilterPrefixes) {
        String allExMsg = GenericTaskException.toExceptionMessageWithScriptConsideration(allErrors,
            includeStackTraces,
            stackTraceLimitPrefixes, stackTraceFilterPrefixes);
        return new MojoExecutionException(allExMsg);
    }
}