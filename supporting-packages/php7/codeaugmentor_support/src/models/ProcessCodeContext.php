<?php declare(strict_types=1);
namespace aaronicsubstances\codeaugmentor_support\models;

class ProcessCodeContext {

   public $globalScope = [];
   public $fileScope = [];
   public $fileAugCodes;
   public $augCodeIndex = 0;
   public $srcFile;
   
    public function newGenCode() {
        return new GeneratedCode;
    }

    public function newContent(string $content, bool $exactMatch = FALSE) : ContentPart    {
        return new ContentPart($content, $exactMatch);
    }
    
    public function __toString() {
        return get_class($this) . ": " . $this->srcFile;
    }
}