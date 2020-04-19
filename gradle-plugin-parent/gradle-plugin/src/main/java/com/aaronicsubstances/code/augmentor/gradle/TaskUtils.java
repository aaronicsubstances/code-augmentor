package com.aaronicsubstances.code.augmentor.gradle;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;

import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

public class TaskUtils {
    
    public static BiConsumer<GenericTaskLogLevel, Supplier<String>> createLogAppender(Task task, boolean verboseEnabled) {
        BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            Logger logger = task.getProject().getLogger();
            switch (logLevel) {
                case VERBOSE:
                    if (!verboseEnabled) {
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

    public static GradleException convertToPluginException(List<Throwable> allErrors) {
        return convertToPluginException(allErrors, false, false, null, null);
    }

    public static GradleException convertToPluginException(List<Throwable> allErrors,
            boolean includeStackTraces, boolean useDefaultGroovyPrefixes, 
            List<String> stackTraceFilterPrefixes, List<String> stackTraceLimitPrefixes) {
        String allExMsg = GenericTaskException.toExceptionMessageWithScriptConsideration(allErrors,
            includeStackTraces, useDefaultGroovyPrefixes,
            stackTraceLimitPrefixes, stackTraceFilterPrefixes);
        return new GradleException(allExMsg);
    }
}