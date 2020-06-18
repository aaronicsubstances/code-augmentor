@GrabConfig(systemClassLoader=true) // needed so -cp deps can find grabbed deps
@Grab('com.google.code.gson:gson:2.8.6')

// import scripts with functions.
// fortunately in Groovy, we only have to do this for scripts
// not in default package (just as this script is in the default package).

import groovy.json.*;
import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;

def parentTask = new ProcessCodeGenericTask()
parentTask.inputFile = new File(args[0])
parentTask.outputFile = new File(args[1])
boolean verboseLoggingEnabled = false
if (args.size() > 2) {
    verboseLoggingEnabled = Boolean.parseBoolean(args[2])
}
parentTask.logAppender = { logLevel, msgSupplier ->
    switch (logLevel) {
        case GenericTaskLogLevel.VERBOSE:
            if (verboseLoggingEnabled) {
                println(msgSupplier.get());
            }
            break;
        case GenericTaskLogLevel.INFO:
            println(msgSupplier.get());
            break;
        case GenericTaskLogLevel.WARN:
            System.err.println(msgSupplier.get());
            break;
    }
}
def jsonParser = new JsonSlurper()
parentTask.jsonParseFunction = {
    return jsonParser.parseText(it)
}
final FUNCTION_NAME_REGEX = /^((Snippets|Worker)\.)[a-zA-Z]\w*$/
parentTask.validationCallback = { functionName, augCode, context ->
    if (functionName ==~ FUNCTION_NAME_REGEX) {
        return null
    }
    return "Invalid/Unsupported function name: " + functionName
}
parentTask.evalFunction = { functionName, augCode, context ->
    binding.augCode = augCode
    binding.context = context
    return evaluate(functionName + '(augCode, context)')
}
parentTask.execute()
if (parentTask.allErrors) {
    String errMsg = PluginUtils.stringifyPossibleScriptErrors(parentTask.allErrors, true,
        [ getClass().name ], null)
    System.err.println(errMsg)
    System.exit(1)
}