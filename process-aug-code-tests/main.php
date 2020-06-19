<?php declare(strict_types=1);
require_once 'build/php7/ProcessCodeTask.php';
require_once 'build/php7/ProcessCodeContext.php';
require_once 'build/php7/CodeAugmentorFunctions.php';
require_once 'Snippets.php';

$instance = new \aaronicsubstances\code_augmentor_support\ProcessCodeTask();
$instance->inputFile = $argv[1];
$instance->outputFile = $argv[2];
if ($argc > 3) {
    $instance->verbose = !!$argv[3];
}

$FUNCTION_NAME_REGEX = '/^(((.*CodeAugmentorFunctions)|Snippets|Worker)\\.)[a-zA-Z]\\w*$/';
$instance->execute(function($functionName, $augCode, $context) use ($FUNCTION_NAME_REGEX) {
    // validate name.
    if (!preg_match($FUNCTION_NAME_REGEX, $functionName)) {
        throw new \Exception("Invalid/Unsupported function name: " . $functionName);
    }

    // name is valid but replace . with :: in function name.
    $functionName = str_replace('.', '::', $functionName);
    // now make function call "dynamically".
    $result = call_user_func($functionName, $augCode, $context);
    return $result;
});

if ($instance->allErrors) {
    fwrite(STDERR, count($instance->allErrors) . " error(s) found." . PHP_EOL,);
    foreach ($instance->allErrors as $errMsg) {
        fwrite(STDERR, $errMsg . PHP_EOL);
    }
    exit(1);
}