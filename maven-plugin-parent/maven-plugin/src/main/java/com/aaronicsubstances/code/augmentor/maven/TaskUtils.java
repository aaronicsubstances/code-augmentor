package com.aaronicsubstances.code.augmentor.maven;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;

import org.apache.maven.plugin.AbstractMojo;
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
}