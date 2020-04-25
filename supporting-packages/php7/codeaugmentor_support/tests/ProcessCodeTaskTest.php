<?php declare(strict_types=1);
use PHPUnit\Framework\TestCase;

use aaronicsubstances\codeaugmentor_support\tasks\ProcessCodeTask;

final class ProcessCodeTaskTest extends TestCase
{
    public function testBasicUsage(): void {
        $task = new ProcessCodeTask;
        $task->inputFile = __DIR__ . DIRECTORY_SEPARATOR .'basic_usage_aug_codes.json';
        $tmpdir = sys_get_temp_dir();
        $task->outputFile = $tmpdir . DIRECTORY_SEPARATOR . 'basic_usage_gen_codes.json';
        
        // print blank line so execute output comes out nicely
        print PHP_EOL;
        $task->execute(function($functionName, $augCode, $context) {
            //return call_user_func($functionName, $augCode, $context);
            $augCodeStr = print_r($augCode, TRUE);
            return "Received: $functionName: $augCodeStr, $context";
        });
        $this->assertEmpty($task->allErrors, print_r($task->allErrors, true));
        print "Output successfully written to {$task->outputFile}";
    }
}