package com.aaronicsubstances.code.augmentor.gradle;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;

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
}