import fs from "fs/promises"
import path from "path"
import { AstBuilder } from "./AstBuilder"
import { DefaultAstTransformer } from "./DefaultAstTransformer"
import {
    AugmentedSourceCode,
    AugmentingCodeDescriptor,
    GeneratedCode,
    GeneratedCodePart,
    SourceCodeAst,
    SourceFileDescriptor
} from "./types"
import * as helperUtils from "./helperUtils"

export class DefaultCodeGenerationStrategy {
    verbose: boolean = true
    parser?: AstBuilder
    transformer?: DefaultAstTransformer
    defaultFileEncoding?: BufferEncoding
    codeGenerator?: (augCode: AugmentingCodeDescriptor,
        context: DefaultCodeGenerationStrategy) => Promise<any>
    globalScope: {}
    fileScope: {
        srcPath: string,
        srcAst: SourceCodeAst,
        augSourceCode: AugmentedSourceCode,
        augCodeIndex: number,
        genCodes: Array<GeneratedCode>,
        generatedCodeInserted: boolean
    }

    constructor() {
        this.globalScope = {
            code_indent: ''.padEnd(4)
        }
        this.fileScope = {
            srcPath: '',
            srcAst: { type: AstBuilder.TYPE_SOURCE_CODE, children: [] },
            augSourceCode: { augCodes: [], parts: [] },
            augCodeIndex: 0,
            genCodes: [],
            generatedCodeInserted: false
        }
    }

    async *generateSrcFileDescriptors(
            files: any, srcDir: string, fileEncoding?: BufferEncoding) {
        let startTime = 0
        if (this.verbose) {
            startTime = new Date().getTime()
            console.log (`\nIn directory ${srcDir}:\n`)
        }
        let idx = 0 
        for await (const relativePath of files) {
            const srcPath = path.join(srcDir, relativePath)
            idx++
            if (this.verbose) {
                console.log(`${idx}. Processing file ${relativePath}...`)
            }
            const effectiveEncoding = fileEncoding || this.defaultFileEncoding || "utf8"
            let fileContent = await fs.readFile(srcPath,
                effectiveEncoding) as string
            const parser = this.parser
            if (!parser) {
                throw new Error("parser property not set")    
            }
            const srcAst = parser.parse(fileContent, srcPath)

            fileContent = await this.applyTransforms(srcAst, srcPath)

            const srcFileDescriptor: SourceFileDescriptor = {
                baseDir: srcDir,
                relativePath: relativePath,
                content: fileContent,
                isBinary: false,
                encoding: effectiveEncoding
            }
            yield srcFileDescriptor
        }
        if (this.verbose) {
            let timeTaken = new Date().getTime() - startTime
            let timeUnit = "ms"
            if (timeTaken >= 5000) {
                timeTaken = timeTaken / 1000.0
                timeUnit = "s"
            }
            console.log (`\nDone processing ${idx} files in directory ${srcDir} ` +
                `in ${timeTaken.toFixed(2)} ${timeUnit}\n`)
        }
    }

    getParts(augCode: AugmentingCodeDescriptor) {
        const parts = this.fileScope.augSourceCode.parts;
        return parts.slice(augCode.leadPartIdx,
            augCode.leadPartIdx + augCode.partCount)
    }
    
    async applyTransforms(srcAst: SourceCodeAst, srcPath: string) {        
        // reset fileScope and re-initialize global scope if needed
        if (!this.globalScope) {
            this.globalScope = {                
                code_indent: ''.padEnd(4)
            }
        }
        this.fileScope = {
            srcPath,
            srcAst,
            augSourceCode: { augCodes: [], parts: [] },
            augCodeIndex: 0,
            genCodes: [],
            generatedCodeInserted: false
        }
        const context = this.fileScope
        const transformer = this.transformer
        if (!transformer) {
            throw new Error("transformer property not set")
        }
        const augSourceCode = transformer.extractAugmentedSourceCode(
            context.srcAst)
        context.augSourceCode = augSourceCode
        const augCodes = context.augSourceCode.augCodes

        // call generating functions twice, first before code generation, 
        // and second after code generation.
        context.generatedCodeInserted = false
            
        const callComputeGenCodesAgainLog = new Array<boolean>()
        for (let i = 0; i < augCodes.length; i++) {
            context.augCodeIndex = i
            context.genCodes = [] // reset
            const augCode = augCodes[i]

            const codeGenerator = this.codeGenerator
            if (!codeGenerator) {
                throw new Error("codeGenerator property not set")
            }
            const callComputeGenCodesAgain = await codeGenerator(augCode, this)
            const genCodes = await DefaultCodeGenerationStrategy.cleanGenCodeList(
                context.genCodes)
            transformer.insertGeneratedCode(augSourceCode, augCode,  genCodes)
            callComputeGenCodesAgainLog.push(!!callComputeGenCodesAgain)
        }

        // call interested functions again after code generation.
        context.generatedCodeInserted = true
        for (let i = 0; i < augCodes.length; i++) {
            context.augCodeIndex = i
            context.genCodes = [] // reset
            const augCode = augCodes[i]

            if (callComputeGenCodesAgainLog[i]) {
                const codeGenerator = this.codeGenerator
                if (!codeGenerator) {
                    throw new Error("codeGenerator property not set")
                }
                await codeGenerator(augCode, this)
            }
        }
        return DefaultAstTransformer.serializeSourceCodeParts(augSourceCode.parts)
    }

    static cleanGenCodeList(result: any):
            Array<GeneratedCode | null> | Promise<Array<GeneratedCode | null>> {
        if (helperUtils._requiresForAwaitIteration(result)) {
            return DefaultCodeGenerationStrategy._cleanGenCodeListAsync(result)
        }
        const converted = new Array<GeneratedCode | null>()
        if (Array.isArray(result) || result[Symbol.iterator]) {
            for (const item of result) {
                const genCode = DefaultCodeGenerationStrategy._convertGenCode(item)
                converted.push(genCode)
            }
        }
        else {
            const genCode = DefaultCodeGenerationStrategy._convertGenCode(result)
            converted.push(genCode)
        }
        return converted
    }

    static async _cleanGenCodeListAsync(result: any): Promise<Array<GeneratedCode | null>> {
        const converted = new Array<GeneratedCode | null>()
        for await (const item of result) {
            const genCode = DefaultCodeGenerationStrategy._convertGenCode(item)
            converted.push(genCode)
        }
        return converted
    }

    static _convertGenCode(item: any): GeneratedCode | null {
        if (item === null || typeof item === 'undefined') {
            return null
        }
        if (Array.isArray(item.contentParts) || item.contentParts[Symbol.iterator]) {
            const contentParts = new Array<GeneratedCodePart | null>()
            for (const contentPart of item.contentParts) {
                contentParts.push(DefaultCodeGenerationStrategy._convertGenCodePart(
                    contentPart))
            }
            return {
                ...item,
                contentParts
            } as GeneratedCode
        }
        const part = DefaultCodeGenerationStrategy._convertGenCodePart(item)
        return {
            ...item,
            contentParts: part ? [ part ] : null
        } as GeneratedCode
    }

    static _convertGenCodePart(item: any): GeneratedCodePart | null {
        if (item === null || typeof item === 'undefined') {
            return null
        }
        if (item.content !== null && typeof item.content !== 'undefined') {
            return {
                ...item,
                content: `${item.content}`
            } as GeneratedCodePart
        }
        return {
            ...item,
            content: `${item}`
        } as GeneratedCodePart
    }

    wrapError(cause: any, message?: string) {
        const context = this.fileScope
        let wrapperMessage = ''
        let srcFileSnippet = null
        if (context) {
            try {
                if (context.srcPath) {
                    wrapperMessage += `in ${context.srcPath}`
                }
                if (context.augSourceCode) {
                    const augCode = context.augSourceCode.augCodes[
                        context.augCodeIndex]
                    wrapperMessage += ` at line ${augCode.leadPart.lineNumber}`
                    srcFileSnippet = augCode.leadNode.indent +
                        augCode.leadNode.marker + augCode.leadNode.markerAftermath
                }
            }
            catch (ignore) {}
        }
        if (wrapperMessage) {
            wrapperMessage += ": "
        }
        if (message !== null && typeof message !== "undefined") {
            wrapperMessage += message
        }
        if (srcFileSnippet) {
            wrapperMessage += `\n\n${srcFileSnippet}`
        }
        const wrapperException = new Error(wrapperMessage)
        if (cause) {
            wrapperException.cause = cause
        }
        return wrapperException
    }
}
