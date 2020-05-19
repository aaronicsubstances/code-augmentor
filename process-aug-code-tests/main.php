<?php declare(strict_types=1);
require_once 'build/php7/models/ContentPart.php';
require_once 'build/php7/models/GeneratedCode.php';
require_once 'build/php7/models/ProcessCodeContext.php';
require_once 'build/php7/tasks/ProcessCodeTask.php';
require_once 'Snippets.php';

$instance = new \aaronicsubstances\code_augmentor_support\tasks\ProcessCodeTask();
$instance->inputFile = $argv[1];
$instance->outputFile = $argv[2];
if ($argc > 2) {
    $instance->verbose = !!$argv[3];
}
assert(!!$instance->inputFile);
assert(!!$instance->outputFile);

$FUNCTION_NAME_REGEX = '^((Snippets|Worker)\\.)[a-zA-Z]\\w*$';
$instance->execute(function($functionName, $augCode, $context) {
    // validate name.
    if (!preg_match($FUNCTION_NAME_REGEX, $functionName)) {
        throw new \Exception("Invalid/Unsupported function name: " + $functionName);
    }

    // name is valid. make function call "dynamically".
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