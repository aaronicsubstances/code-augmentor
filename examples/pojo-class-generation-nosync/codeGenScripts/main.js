const fs = require("fs/promises")
const path = require("path")
const { globIterate }  = require("glob")
const {
    CodeChangeDetective,
    DefaultCodeChangeDetectiveConfigFactory,
    AstBuilder,
    DefaultAstTransformer,
    AstFormatter
} = require("code-augmentor-support")
const OtherFunctions = require("./OtherFunctions")
const customCodeGeneration = require("./CustomCodeGeneration")

// index 0 is for path to node program
// index 1 is for path to main js file
const srcDir = process.argv[2]
const destDir = process.argv[3]

const parser = new AstBuilder()
parser.decoratedLineMarkers = [":AUG_CODE:", ":JSON:"]
parser.escapedBlockStartMarkers = ["", ""] 
parser.escapedBlockEndMarkers = ["", ""]
parser.nestedBlockStartMarkers = ["", ""]
parser.nestedBlockEndMarkers = ["", ""]

const transformer = new DefaultAstTransformer()
transformer.augCodeMarkers = [":AUG_CODE:"] // decoratedLine
transformer.augCodeArgMarkers = ["", ""] // decoratedLine or escapedBlockStart
transformer.augCodeJsonArgMarkers = [":JSON:"] // decoratedLine or escapedBlockStart
transformer.genCodeMarkers = ["", ""] // decoratedLine or escapedBlockStart

transformer.defaultGenCodeMarker = ""

const context = {
    globalScope: {
        code_indent: ''.padEnd(4)
    },
    fileScope: {
        srcPath: '',
        srcAst: null,
        augCodes: [],
        lineObjects: [],
        augCodeIndex: 0,
        generatedCodesMerged: false
    }
}
const applyTransforms = function() {
    const result = transformer.extractAugCodes(context.fileScope.srcAst)
    Object.assign(context.fileScope, result)
    const augCodes = context.fileScope.augCodes;
    const lineObjects = context.fileScope.lineObjects;

    // call twice, first before code generation, 
    // and second after code generation.
    for (let i = 0; i < 2; i++) {
        for (let i = 0; i < augCodes.length; i++) {
            context.fileScope.augCodeIndex = i
            context.fileScope.genCodes = [] // reset
            const augCode = augCodes[i]
            if (i > 0) {
                context.fileScope.generatedCodesMerged = true;
                customCodeGeneration.computeGenCodes(augCode, context)
            }
            else {
                context.fileScope.generatedCodesMerged = false;
                customCodeGeneration.computeGenCodes(augCode, context)
                transformer.applyGeneratedCodes(augCode, lineObjects, context.fileScope.genCodes)
            }
        }
    }
    return DefaultAstTransformer.performTransformations(lineObjects);
}

const generateSrcFileDescriptors = async function*() {
    const files = globIterate(["**/*.java"], {
        cwd: srcDir,
        nodir: true,
        ignore: 'node_modules/**' // just to demonstrate if js files were being processed.
    })

    for await (const relativePath of files) {
        console.log(`Processing ${relativePath}...`)
        const srcPath = path.join(srcDir, relativePath)
        let fileContent = await fs.readFile(srcPath, "utf8")
        let srcAst = parser.parse(fileContent, srcPath)

        // reset file scope of context
        context.fileScope = {
            srcAst,
            srcPath,
            augCodesToRemove: [],
        }
        fileContent = applyTransforms()

        /*interface SourceFileDescriptor {
            baseDir?: string;
            relativePath: string;
            content?: string;
            encoding?: BufferEncoding;
            binaryContent?: Buffer;
            isBinary: boolean;
        }*/
        yield {
            baseDir: srcDir,
            relativePath: relativePath,
            content: fileContent
        }
    }
}


const configFactory = new DefaultCodeChangeDetectiveConfigFactory()
configFactory.destDir = destDir
const codeChangeDetective = new CodeChangeDetective()
codeChangeDetective.configFactory = configFactory
codeChangeDetective.srcFileDescriptors = generateSrcFileDescriptors()
codeChangeDetective.codeChangeDetectionEnabled = false
codeChangeDetective.reportError = function(cause, message) {
    let wrapperMessage = '';
    let srcFileSnippet = null;
    try {
        if (context.fileScope.srcPath) {
            wrapperMessage += `in ${context.fileScope.srcFile}`;
        }
        if (context.fileScope.augCodes) {
            const augCode = context.fileScope.augCodes[context.fileScope.augCodeIndex];
            const augCodeNode = OtherFunctions.getAugCodeNodeStart(augCode)
            wrapperMessage += ` at line ${augCodeNode.lineNumber}`;
            srcFileSnippet = augCodeNode.marker + augCodeNode.markerAftermath
        }
    }
    catch (ignore) {}
    if (wrapperMessage) {
        wrapperMessage += ": ";
    }
    if (message !== null && message !== undefined) {
        wrapperMessage += message;
    }
    if (srcFileSnippet) {
        wrapperMessage += `\n\n${srcFileSnippet}`;
    }
    const wrapperException = new Error(wrapperMessage);
    if (cause) {
        wrapperException.cause = cause;
    }
    throw wrapperException; // could also log and print out after execution
};

// start execution
(async function() {
    await codeChangeDetective.execute()
})()
