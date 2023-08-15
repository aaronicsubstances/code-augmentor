const path = require("path")
const { globIterate }  = require("glob")
const {
    AstParser,
    CodeChangeDetective,
    DefaultAstTransformer,
    DefaultCodeChangeDetectiveConfigFactory,
    DefaultCodeGenerationStrategy
} = require("code-augmentor-support")
const CustomCodeGeneration = require("./CustomCodeGeneration")

// index 0 is for path to node program
// index 1 is for path to main.js file
//const srcDir = process.argv[2]
//const destDir = process.argv[3]
const srcDir = path.join(path.dirname(__dirname), "src")
console.log(srcDir)
const destDir = path.join(__dirname, "generated")
console.log(destDir)

const parser = new AstParser()
parser.decoratedLineMarkers = ["//:AUG_CODE:"]
parser.escapedBlockStartMarkers = ["//:JSON:"]
parser.escapedBlockEndMarkers = ["//:JSON:"]
parser.nestedBlockStartMarkers = ["", ""]
parser.nestedBlockEndMarkers = ["", ""]

const transformer = new DefaultAstTransformer()
transformer.augCodeMarkers = ["//:AUG_CODE:"] // decoratedLine or escapedBlockStart
transformer.augCodeArgMarkers = ["//:JSON:"] // decoratedLine or escapedBlockStart
transformer.genCodeMarkers = ["", ""] // decoratedLine or escapedBlockStart

transformer.defaultGenCodeInlineMarker = ""
transformer.defaultGenCodeStartMarker = ""
transformer.defaultGenCodeEndMarker = ""

const codeGenStrategy = new DefaultCodeGenerationStrategy()
codeGenStrategy.parser = parser
codeGenStrategy.transformer = transformer
codeGenStrategy.codeGenerator = CustomCodeGeneration.computeGenCodes

const files = globIterate(["**/*.java"], {
    cwd: srcDir,
    nodir: true,
    ignore: 'node_modules/**' // just to demonstrate if js files were being processed.
})

const configFactory = new DefaultCodeChangeDetectiveConfigFactory()
configFactory.destDir = destDir
const codeChangeDetective = new CodeChangeDetective()
codeChangeDetective.configFactory = configFactory
codeChangeDetective.srcFileDescriptors = codeGenStrategy.generateSrcFileDescriptors(
    files, srcDir)
codeChangeDetective.codeChangeDetectionEnabled = false
codeChangeDetective.reportError = function(cause, message) {
    throw codeGenStrategy.wrapError(cause, message)
}

// start execution
/*(async function() {
    await codeChangeDetective.execute()
})()*/
codeChangeDetective.execute()