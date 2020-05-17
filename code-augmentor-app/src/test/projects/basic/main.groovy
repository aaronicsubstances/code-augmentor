import groovy.json.*;

public class Main {
    static final FUNCTION_NAME_REGEX = ~/^((Snippets|Worker)\.)[a-zA-Z]\w*$/;
    static final jsonParser = new JsonSlurper();
    static final GroovyShell shell = new GroovyShell()
    
    static verboseLoggingEnabled = false

    static void main(String[] args) {
        def inputFile = new File(args[0]);
        def inputReader = inputFile.newReader('utf-8');
        def headerLine = null;
    
        def outputFile = new File(args[1]);
        outputFile.parentFile?.mkdirs();
        def outputWriter = outputFile.newPrintWriter('utf-8');
        outputWriter.println("{}");
        
        if (args.size() > 2) {
            verboseLoggingEnabled = Boolean.parseBoolean(args[2])
        }

        def context = [
            globalScope: [:],
            newGenCode: {
                return [
                    id: 0,
                    contentParts: []
                ];
            },
            newContent: { content, exactMatch=false ->
                return [
                    content: content, 
                    exactMatch: !!exactMatch
                ];
            }
        ];

        def line;
        def allErrors = [];
        while (line = inputReader.readLine()) {
            def parsedLine = parseJson(line);
            if (context.header == null) {
                context.header = parsedLine;
                continue;
            }
                
            def fileAugCodes = parsedLine;

            // set up context.
            context.srcFile = fileAugCodes.dir + File.separator +
                fileAugCodes.relativePath;
            context.fileAugCodes = fileAugCodes;
            context.fileScope = [:];
            logVerbose("Processing ${context.srcFile}");

            // fetch arguments, and parse any json argument found.
            for (augCode in fileAugCodes.augmentingCodes) {
                augCode.processed = false;
                augCode.args = [];
                for (block in augCode.blocks) {
                    if (block.jsonify) {
                        def parsedArg = parseJson(block.content);
                        augCode.args << parsedArg;
                    }
                    else if (block.stringify) {
                        augCode.args << block.content;
                    }
                }
            }
            
            // now begin aug code processing.
            def fileGenCodes = [
                fileId: fileAugCodes.fileId,
                generatedCodes: []
            ];
            def beginErrorCount = allErrors.size();
            for (int i = 0; i < fileAugCodes.augmentingCodes.size(); i++) {
                def augCode = fileAugCodes.augmentingCodes[i];
                if (augCode.processed) {
                    continue;
                }

                context.augCodeIndex = i;
                def functionName = augCode.blocks[0].content.trim();
                def genCodes = processAugCode(functionName,
                    augCode, context, allErrors);
                for (genCode in genCodes) {
                    fileGenCodes.generatedCodes.add(genCode);
                }
            }
                        
            validateGeneratedCodeIds(fileGenCodes.generatedCodes, context, allErrors);
            
            if (allErrors.size() > beginErrorCount) {
                logWarn((allErrors.size() - beginErrorCount) +
                    " error(s) encountered in " + context.srcFile);
            }            

            if (!allErrors) {
                outputWriter.println(toJson(fileGenCodes));
            }
            logInfo("Done processing " + context.srcFile);
        }
        outputWriter.close();
        inputReader.close();

        if (allErrors) {
            System.err.println("${allErrors.size()} error(s) were found:\n")
            for (err in allErrors) {
                System.err.println(err)
            }
            System.exit(1)
        }
    }

    static parseJson(text) {
        return jsonParser.parseText(text)
    }

    static toJson(obj) {
        def asJson = JsonOutput.toJson(obj)
        return asJson
    }

    static logVerbose(msg) {
        if (verboseLoggingEnabled) {
            println(msg);
        }
    }

    static logInfo(msg) {
        println(msg);
    }

    static logWarn(msg) {
        System.err.println(msg);
    }

    static processAugCode(functionName, augCode, context,
            allErrors) {
        try {
            if (!FUNCTION_NAME_REGEX.matcher(functionName).matches()) {
                throw new IllegalArgumentException("Invalid/Unsupported function name: " + functionName)
            }
            shell.context.augCode = augCode;
            shell.context.context = context
            def result = shell.evaluate(functionName + "(augCode, context)")
            if (result == null) {
                return [];
            }
            def converted = [];
            if (result instanceof Collection || result instanceof Object[]) {
                for (item in result) {
                    def genCode = convertGenCodeItem(item);
                    converted.add(genCode);
                    // try and mark corresponding aug code as processed.
                    if (genCode.id > 0) {
                        def correspondingAugCode = 
                            context.fileAugCodes.augmentingCodes
                                .find { it.id == genCode.id }
                        if (correspondingAugCode) {
                            correspondingAugCode.processed = true;
                        }
                    }
                }
            }
            else {
                def genCode = convertGenCodeItem(result);
                genCode.id = augCode.id;
                converted.add(genCode);
            }
            return converted;
        }
        catch (excObj) {
            createException(context, null, excObj, allErrors);
            return [];
        }
    }

    static convertGenCodeItem(item) {
        if (item == null) {
            return [ id: 0 ];
        }
        if (item instanceof Map && item.contentParts != null) {
            // assume it is GeneratedCode instance and ensure
            // existence of id field.
            if (!item.id) {
                item.id = 0;
            }
            return item;
        }
        else if (item instanceof Map && item.content != null) {
            // assume it is ContentPart instance
            return [
                id: 0,
                contentParts: [ item ]
            ];
        }
        else {
            // assume string or stringify it.
            return [
                id: 0,
                contentParts: [
                    [
                        content: "${item}",
                        exactMatch: false
                    ]
                ]
            ];
        }
    }

    static validateGeneratedCodeIds(fileGenCodeList, context, allErrors) {
        def ids = fileGenCodeList.collect { it.id }
        // Interpret use of -1 or negatives as intentional and skip
        // validating negative ids.
        if (!ids.every()) {
            createException(context, 'At least one generated code id was not set. Found: ' + ids,
                null, allErrors);
        }
        else {
            if (ids.unique(false).size() < ids.size()) {
                createException(context, 'Valid generated code ids must be unique, but found duplicates: ' + ids,
                    null, allErrors);
            }
        }
    }

    static createException(context, message, evalEx, allErrors) {
        def lineMessage = '';
        def stackTrace = '';
        if (evalEx) {
            def augCode = context.fileAugCodes.augmentingCodes[context.augCodeIndex];
            lineMessage = " at line ${augCode.lineNumber}";
            message = augCode.blocks[0].content;
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            evalEx.printStackTrace(pw)
            pw.flush()
            stackTrace = '\n' + sw
        }
        def exception = "in ${context.srcFile}${lineMessage}: ${message}${stackTrace}";
        allErrors.add(exception);
    }
}