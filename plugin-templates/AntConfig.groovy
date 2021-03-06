PLUGIN_BASE_TASK_TYPE = 'Task'
PLUGIN_FILE_SET_TYPE = 'FileSet'
PLUGIN_EXECUTION_EXCEPTION_TYPE = 'BuildException'
PLUGIN_PREPARE_TASK_TYPE = 'PrepareTask'
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
        if (resolvedInlineGenCodeDirectives.isEmpty()) {
            resolvedInlineGenCodeDirectives.add("/*:GEN_CODE:*/");
        }
        if (resolvedNestedLevelStartMarkers.isEmpty()) {
            resolvedNestedLevelStartMarkers.add("{");
        }
        if (resolvedNestedLevelEndMarkers.isEmpty()) {
            resolvedNestedLevelEndMarkers.add("}");
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

COMPLETION_FILES_VALIDATION_CODE = '''// set up defaults
        if (resolvedPrepFile == null) {
            resolvedPrepFile = TaskUtils.getDefaultPrepFile(task);
        }
        if (resolvedGenCodeFiles.isEmpty()) {
            resolvedGenCodeFiles.add(TaskUtils.getDefaultGenCodeFile(task));
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

LOG_REFERENCE = ''
LOG_NO_FILES_FOUND = 'task.log("No files were found", Project.MSG_WARN);'
LOG_ONE_OR_MORE_FILE_COUNT = 'task.log(String.format("Found %s file(s)", relativePaths.size()));'

// use equivalent of 3 tabs
LOG_PREPARE_TASK_PROPERTIES = """task.log("Configuration properties:");
            task.log("\\tencoding: " + genericTask.getCharset());
            task.log("\\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
            task.log("\\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
            task.log("\\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
            task.log("\\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
            task.log("\\tskipCodeStartDirectives: " + genericTask.getSkipCodeStartDirectives());
            task.log("\\tskipCodeEndDirectives: " + genericTask.getSkipCodeEndDirectives());
            task.log("\\tinlineGenCodeDirectives: " + genericTask.getInlineGenCodeDirectives());
            task.log("\\tnestedLevelStartMarkers: " + genericTask.getNestedLevelStartMarkers());
            task.log("\\tnestedLevelEndMarkers: " + genericTask.getNestedLevelEndMarkers());
            
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

            task.log("\\tgenericTask.logAppender: " + genericTask.getLogAppender());
            task.log("\\tgenericTask.baseDirs: " + new HashSet<>(genericTask.getBaseDirs()));
            // ant's FileSet.toString() prints relative paths of its files
            task.log("\\tsrcDirs: " + resolvedFileSets);"""

LOG_COMPLETE_TASK_PROPERTIES = """task.log("Configuration properties:");
            task.log("\\tdestDir: " + genericTask.getDestDir());
            if (task instanceof $PLUGIN_COMPLETE_TASK_TYPE) {
                task.log("\\tprepFile: " + genericTask.getPrepFile());
                for (int i = 0; i < genericTask.getGeneratedCodeFiles().size(); i++) {
                    task.log("\\tgenCodeSpecs[" + i + "].file: " + genericTask.getGeneratedCodeFiles().get(i));
                }
            }
            task.log("\\tcodeChangeDetectionDisabled: " + genericTask.isCodeChangeDetectionDisabled());
            task.log("\\tfailOnChanges: " + resolvedFailOnChanges);
            task.log("\\tgenericTask.logAppender: " + genericTask.getLogAppender());"""