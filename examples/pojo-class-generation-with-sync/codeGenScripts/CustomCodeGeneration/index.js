const MyFunctions = require('./MyFunctions.js');

const FUNCTION_NAME_REGEX = /^((MyFunctions|YourFunctions)\.)[a-zA-Z]\w*$/
exports.computeGenCodes = async function(augCode, context) {
    const functionName = augCode.data[0].trim()
    if (!FUNCTION_NAME_REGEX.test(functionName)) {
        throw new Error("Invalid/Unsupported function name: " + functionName)
    }

    // name is valid. make function call "dynamically".
    return await eval(functionName + "(augCode, context)")
}
