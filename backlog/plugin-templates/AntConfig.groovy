PLUGIN_BASE_TASK_TYPE = 'Task'
PLUGIN_FILE_SET_TYPE = 'FileSet'
PLUGIN_EXECUTION_EXCEPTION_TYPE = 'BuildException'
PLUGIN_PREPARE_TASK_TYPE = 'PrepareTask'
PLUGIN_PROCESS_TASK_TYPE = 'ProcessTask'
PLUGIN_COMPLETE_TASK_TYPE = 'CompletionTask'

VAL_ERROR_NO_FILESET = '"at least 1 element is required in srcDirs"'
VAL_ERROR_NO_GEN_CODE_START_DRV = '"at least 1 element is required in genCodeStartDirectives"'
VAL_ERROR_NO_GEN_CODE_END_DRV = '"at least 1 element is required in genCodeEndDirectives"'
VAL_ERROR_PREPARE_NO_AUG_CODE_SPEC = '"at least 1 element is required in augCodeSpecs"'
VAL_ERROR_RUN_NO_AUG_CODE_SPEC = '"unexpected absence of augCodeDirectives"'
VAL_ERROR_PREPARE_NO_AUG_CODE_DRV_AT_I = '"at least 1 element is required in augCodeSpecs[" + i + "].directives"'
VAL_ERROR_RUN_NO_AUG_CODE_DRV_AT_I = '"at least 1 element is required in augCodeDirectives"'
VAL_ERROR_PREPARE_NULL_AUG_CODE_FILE_AT_I = '"invalid null value found at augCodeSpecs[" + i + "]?.destFile"'
VAL_ERROR_RUN_NULL_AUG_CODE_FILE = '"unexpected absence of augCodeFile"'
VAL_ERROR_NULL_FILE_SET_AT_I = '"invalid null value found at fileSets[" + i + "]"'
VAL_ERROR_COMPLETE_NULL_GEN_CODE_FILE_AT_I = '"invaid null value found at genCodeSpecs[" + i + "]"'
VAL_ERROR_RUN_NULL_GEN_CODE_FILE = '"unexpected absence of genCodeFile"'
VAL_ERROR_PROCESS_NULL_AUG_CODE_FILE = VAL_ERROR_PREPARE_NULL_AUG_CODE_FILE_AT_I
VAL_ERROR_PROCESS_NULL_GEN_CODE_FILE = VAL_ERROR_COMPLETE_NULL_GEN_CODE_FILE_AT_I
VAL_ERROR_NO_GROOVY_SCRIPT_DIR = '"groovyScriptDir property is required if scriptEvalFunction reference is absent"'

PREPARE_FILES_VALIDATION_CODE = '''// set up defaults
        if (resolvedEncoding == null) {
            resolvedEncoding = "UTF-8";
        }
        if (resolvedGenCodeStartDirectives.isEmpty()) {
            resolvedGenCodeStartDirectives.add("//:GEN_CODE_START:");
        }
        if (resolvedGenCodeEndDirectives.isEmpty()) {
            resolvedGenCodeEndDirectives.add("//:GEN_CODE_END:");
        }
        if (resolvedEmbeddedStringDirectives.isEmpty()) {
            resolvedEmbeddedStringDirectives.add("//:STR:");
        }
        if (resolvedEmbeddedJsonDirectives.isEmpty()) {
            resolvedEmbeddedJsonDirectives.add("//:JSON:");
        }
        if (resolvedSkipCodeStartDirectives.isEmpty()) {
            resolvedSkipCodeStartDirectives.add("//:SKIP_CODE_START:");
        }
        if (resolvedSkipCodeEndDirectives.isEmpty()) {
            resolvedSkipCodeEndDirectives.add("//:SKIP_CODE_END:");
        }
        if (resolvedAugCodeSpecDirectives.isEmpty()) {
            resolvedAugCodeSpecDirectives.add(Arrays.asList("//:AUG_CODE:"));
        }
        if (resolvedAugCodeFiles.isEmpty()) {
            resolvedAugCodeFiles.add(TaskUtils.getDefaultAugCodeFile(task));
        }
        if (resolvedPrepFile == null) {
            resolvedPrepFile = TaskUtils.getDefaultPrepFile(task);
        }'''

PROCESS_FILES_VALIDATION_CODE = '''// set up defaults
        if (resolvedAugCodeFile == null) {
            resolvedAugCodeFile = TaskUtils.getDefaultAugCodeFile(task);
        }
        if (resolvedGenCodeFile == null) {
            resolvedGenCodeFile = TaskUtils.getDefaultGenCodeFile(task);
        }
        if (resolvedGroovyEntryScriptName == null) {
            resolvedGroovyEntryScriptName = "main.groovy";
        }'''

COMPLETION_FILES_VALIDATION_CODE = '''// set up defaults
        if (resolvedEncoding == null) {
            resolvedEncoding = "UTF-8";
        }
        if (resolvedPrepFile == null) {
            resolvedPrepFile = TaskUtils.getDefaultPrepFile(task);
        }
        if (resolvedGenCodeFiles.isEmpty()) {
            resolvedGenCodeFiles.add(TaskUtils.getDefaultGenCodeFile(task));
        }
        if (resolvedChangeSetInfoFile == null) {
            resolvedChangeSetInfoFile = TaskUtils.getDefaultChangeSetInfoFile(task);
        }
        if (resolvedDestDir == null) {
            resolvedDestDir = TaskUtils.getDefaultDestDir(task);
        }'''

// use equivalent of 2 tabs
FILE_SET_EXTRACTION_CODE = '''
        for (FileSet srcDir : resolvedFileSets) {
            DirectoryScanner ds = srcDir.getDirectoryScanner(task.getProject());
            String[] includedFiles = ds.getIncludedFiles();
            for (String filename : includedFiles) {
                baseDirs.add(ds.getBasedir());
                assert !filename.startsWith("/");
                assert !filename.startsWith("\\\\");
                relativePaths.add(filename);
            }
        }'''

DEST_DIR_DELETION_CODE = 'Eval.me("x", resolvedDestDir, "x.deleteDir()");'

LOG_REFERENCE = ''
LOG_NO_FILES_FOUND = 'task.log("No files were found", Project.MSG_WARN);'
LOG_ONE_OR_MORE_FILE_COUNT = 'task.log(String.format("Found %s file(s)", relativePaths.size()));'
LOG_DELETE_DEST_DIR = 'task.log("Deleting contents of " + resolvedDestDir + "...");'
LOG_CALLING_GROOVY = 'task.log("Launching " + resolvedGroovyEntryScriptName + "...");'

// use equivalent of 3 tabs
LOG_PREPARE_TASK_PROPERTIES = """task.log("Configuration properties:");
            task.log("\\tencoding: " + genericTask.getCharset());
            task.log("\\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
            task.log("\\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
            task.log("\\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
            task.log("\\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
            task.log("\\tskipCodeStartDirectives: " + genericTask.getSkipCodeStartDirectives());
            task.log("\\tskipCodeEndDirectives: " + genericTask.getSkipCodeEndDirectives());
            
            if (task instanceof $PLUGIN_PREPARE_TASK_TYPE) {
                task.log("\\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getAugCodeProcessingSpecs().size(); i++) {
                    AugCodeProcessingSpec augCodeSpec = genericTask.getAugCodeProcessingSpecs().get(i);
                    task.log("\\taugCodeSpecs[" + i + "].directives: " + augCodeSpec.getDirectives());
                    task.log("\\taugCodeSpecs[" + i + "].destFile: " + augCodeSpec.getDestFile());
                }
            }
            else {
                task.log("\\taugCodeDirectives: " + 
                    genericTask.getAugCodeProcessingSpecs().get(0).getDirectives());
            }

            task.log("\\tsrcDirs: " + resolvedFileSets);
            task.log("\\tgenericTask.logAppender: " + genericTask.getLogAppender());
            task.log("\\tgenericTask.baseDirs: " + new HashSet<>(genericTask.getBaseDirs()));
            task.log("\\tgenericTask.relativePaths: " + genericTask.getRelativePaths());"""
            
LOG_PROCESS_TASK_PROPERTIES = """task.log("Configuration properties:");
            task.log("\\tgroovyScriptDir: " + resolvedGroovyScriptDir);
            task.log("\\tgroovyEntryScriptName: " + resolvedGroovyEntryScriptName);
            task.log("\\taugCodeFile: " + resolvedAugCodeFile);
            task.log("\\tgenCodeFile: " + resolvedGenCodeFile);
            task.log("\\tscriptEvalFunction: " + resolvedScriptEvalFunction);
            task.log("\\tstackTraceLimitPrefixes: " + resolvedStackTraceLimitPrefixes);
            task.log("\\tstackTraceFilterPrefixes: " + resolvedStackTraceFilterPrefixes);
            task.log("\\tgenericTask.logAppender: " + genericTask.getLogAppender());
            task.log("\\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());"""

LOG_COMPLETE_TASK_PROPERTIES = """task.log("Configuration properties:");
            task.log("\\tencoding: " + genericTask.getCharset());
            task.log("\\tdestDir: " + genericTask.getDestDir());
            if (task instanceof $PLUGIN_COMPLETE_TASK_TYPE) {
                task.log("\\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    task.log("\\tgenCodeSpecs[" + i + "].file: " + genericTask.getGeneratedCodeFiles().get(i));
                }
            }
            task.log("\\tchangeSetInfoFile: " + resolvedChangeSetInfoFile);
            task.log("\\tgenericTask.logAppender: " + genericTask.getLogAppender());"""