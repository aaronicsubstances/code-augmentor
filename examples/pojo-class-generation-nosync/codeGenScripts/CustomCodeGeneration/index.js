const MyFunctions = require('./MyFunctions.js');

const FUNCTION_NAME_REGEX = /^((MyFunctions|YourFunctions)\.)[a-zA-Z]\w*$/
exports.computeGenCodes = function(augCode, context) {
    // retrieve function name from augCode argument
    /*interface AugmentingCodeDescriptor {
        //...
        nestedBlockUsed: boolean;
        lineNumber: number;
        //...
        markerAftermath: string;
        args: any[];
        //...
        endMarkerAftermath: string | null;
        endArgs: any[] | null;
        //...
    }*/
    const functionName = augCode.markerAftermath
    if (!FUNCTION_NAME_REGEX.test(functionName)) {
        throw new Error("Invalid/Unsupported function name: " + functionName)
    }

    // name is valid. make function call "dynamically".
    const result = eval(functionName + "(augCode, context)")
    const genCodes = convertGenCodeItems(result)
    fileScope.genCodes.push(...genCodes)
}

function convertGenCodeItems(result) {
    const converted = [];
    if (Array.isArray(result)) {
        for (item of result) {
            const genCode = convertGenCodeItem(item);
            converted.push(genCode);
        }
    }
    else {
        const genCode = convertGenCodeItem(result);
        converted.push(genCode);
    }
    return converted;
}

function convertGenCodeItem(item) {
    if (item === null || typeof item === 'undefined') {
        return {}
    }
    if (item.contentParts !== null || typeof item.contentParts !== 'undefined') {
        // assume it is
        /*interface GeneratedCode {
            contentParts: GeneratedCodePart[] | null;
            indent: string | null;
            useInlineMarker: boolean;
            ignore: boolean;
            ignoreRemainder: boolean;
        }*/
        return item
    }
    if (item.content !== null || typeof item.content !== 'undefined') {
        // assume it is
        /*interface GeneratedCodePart {
            content: string | null;
            exempt: boolean;
        }*/
        return {
            contentParts: [ item ]
        };
    }
    // at this point either assume item argument is a string,
    // or just stringify it.
    return {
        contentParts: [
            {
                content: `${item}`
            }
        ]
    };
}
