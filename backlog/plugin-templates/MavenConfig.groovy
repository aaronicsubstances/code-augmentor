PLUGIN_BASE_TASK_TYPE = 'AbstractMojo'
PLUGIN_FILE_SET_TYPE = 'FileSet'
PLUGIN_EXECUTION_EXCEPTION_TYPE = 'MojoExecutionException'
PLUGIN_PREPARE_TASK_TYPE = 'PreparationMojo'
VAL_ERROR_NO_FILESET = '"at least 1 element is required in fileSets"'
VAL_ERROR_NO_GEN_CODE_START_DRV = '"at least 1 element is required in genCodeStartDirectives"'
VAL_ERROR_NO_GEN_CODE_END_DRV = '"at least 1 element is required in genCodeEndDirectives"'
VAL_ERROR_PREPARE_NO_AUG_CODE_SPEC = '"at least 1 element is required in augCodeSpecs"'
VAL_ERROR_RUN_NO_AUG_CODE_SPEC = '"unexpected absence of augCodeDirectives"'
VAL_ERROR_PREPARE_NO_AUG_CODE_DRV_AT_I = '"at least 1 element is required in augCodeSpecs[" + i + "].directives"'
VAL_ERROR_RUN_NO_AUG_CODE_DRV_AT_I = '"at least 1 element is required in augCodeDirectives"'
VAL_ERROR_PREPARE_NULL_AUG_CODE_FILE_AT_I = '"invalid null value found at augCodeSpecs[" + i + "]?.destFile"'
VAL_ERROR_RUN_NULL_AUG_CODE_FILE_AT_I = '"unexpected null for augCodeFile"'
VAL_ERROR_NULL_FILE_SET_AT_I = '"fileSets[" + i + "] is null"'
LOG_REFERENCE = 'Log logger = task.getLog();'

LOG_NO_FILES_FOUND = 'logger.warn("No files were found");'
LOG_ONE_OR_MORE_FILE_COUNT = 'logger.info(String.format("Found %s file(s)", relativePaths.size()));'

// use equivalent of 1 tab
FILE_SET_EXTRACTION_CODE = '''
    FileSetManager fileSetManager = new FileSetManager();
    for (FileSet fileset : resolvedFileSets) {
        File baseDir = new File(fileset.getDirectory());
        String[] includedFiles = fileSetManager.getIncludedFiles( fileset );
        for (String includedFile : includedFiles) {
            baseDirs.add(baseDir);
            relativePaths.add(includedFile);
        }
    }'''

// use equivalent of 2 tabs
LOG_TASK_PROPERTIES = '''
        logger.info("Configuration properties:");
        logger.info("\\tencoding: " + genericTask.getCharset());
        logger.info("\\tprepFile: " + genericTask.getPrepFile());
        logger.info("\\tgenCodeStartDirectives: " + genericTask.getGenCodeStartDirectives());
        logger.info("\\tgenCodeEndDirectives: " + genericTask.getGenCodeEndDirectives());
        logger.info("\\tembeddedStringDirectives: " + genericTask.getEmbeddedStringDirectives());
        logger.info("\\tembeddedJsonDirectives: " + genericTask.getEmbeddedJsonDirectives());
        logger.info("\\tenableScanDirectives: " + genericTask.getEnableScanDirectives());
        logger.info("\\tdisableScanDirectives: " + genericTask.getDisableScanDirectives());
        
        if (task instanceof PreparationMojo) {
            logger.info("\\tprepFile: " + genericTask.getPrepFile());
            for (int i = 0; i < genericTask.getAugCodeProcessingSpecs().size(); i++) {
                AugCodeProcessingSpec augCodeSpec = genericTask.getAugCodeProcessingSpecs().get(i);
                logger.info("\\taugCodeSpecs[" + i + "].directives: " + augCodeSpec.getDirectives());
                logger.info("\\taugCodeSpecs[" + i + "].destFile: " + augCodeSpec.getDestFile());
            }
        }
        else {
            logger.info("\\taugCodeDirectives: " + 
                genericTask.getAugCodeProcessingSpecs().get(0).getDirectives());
        }

        logger.info("\\tfileSets: " + fileSets);
        logger.info("\\tgenericTask.logAppender: " + genericTask.getLogAppender());
        logger.info("\\tgenericTask.baseDirs: " + new HashSet<>(genericTask.getBaseDirs()));
        logger.info("\\tgenericTask.relativePaths: " + genericTask.getRelativePaths());'''