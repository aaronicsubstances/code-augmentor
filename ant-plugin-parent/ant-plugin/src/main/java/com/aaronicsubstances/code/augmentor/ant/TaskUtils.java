package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class TaskUtils {
    
    public static BiConsumer<GenericTaskLogLevel, Supplier<String>> createLogAppender(Task task, boolean verboseEnabled) {
        BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender = (logLevel, msgFunc) -> {
            switch (logLevel) {
                case VERBOSE:
                    if (!verboseEnabled) {
                        break;
                    }
                    else {
                        // fall through.
                    }
                case INFO:
                    task.log(msgFunc.get());
                    break;
                case WARN:
                    task.log(msgFunc.get(), Project.MSG_WARN);
                    break;
            }
        };
        return logAppender;
    }

    public static BuildException convertToPluginException(List<Throwable> allErrors) {
        return convertToPluginException(allErrors, false, null, null);
    }

    public static BuildException convertToPluginException(List<Throwable> allErrors,
            boolean includeStackTraces, 
            List<String> stackTraceLimitPrefixes, List<String> stackTraceFilterPrefixes) {
        String allExMsg = GenericTaskException.toExceptionMessageWithScriptConsideration(allErrors,
            includeStackTraces,
            stackTraceLimitPrefixes, stackTraceFilterPrefixes);
        return new BuildException(allExMsg);
    }

    public static File getDefaultBuildDir(Task task) {
        File defaultBuildDir = new File(new File(task.getProject().getBaseDir(), "build"),
            "codeAugmentor");
        return defaultBuildDir;
    }

    public static File getDefaultPrepFile(Task task) {
        return new File(getDefaultBuildDir(task), "parseResults.json");
    }

    public static File getDefaultAugCodeFile(Task task) {
        return new File(getDefaultBuildDir(task), "augCodes.json");
    }

    public static File getDefaultGenCodeFile(Task task) {
        return new File(getDefaultBuildDir(task), "genCodes.json");
    }

	public static File getDefaultChangeSetInfoFile(Task task) {
		return new File(getDefaultBuildDir(task), "changeSet.txt");
	}

	public static File getDefaultDestDir(Task task) {
		return new File(getDefaultBuildDir(task), "generated");
	}
}