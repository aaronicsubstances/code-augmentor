<?php  declare(strict_types=1);

class Snippets {

    public static function generateSerialVersionUID($augCode, $context) {
        return "private static final int serialVersionUID = 23L;";
    }
    
    public static function stringify($augCode, $context) {
        $g = $context->newGenCode();
        for ($i = 0; $i < count($augCode['args']); $i++) {
            $s = '"' . $augCode['args'][$i];
            if ($i < count($augCode['args']) - 1) {
                $s .= $augCode['lineSeparator'] . '" +';
            }
            else {
                $s .= '"';
            }
            $g->contentParts[] = $context->newContent($s, TRUE);
        }
        return $g;
    }
    
    public static function generateGetter($augCode, $context) {
        $name = $augCode['args'][0]['name'];
        $type = $augCode['args'][0]['type'];
        $nameCapitalized = ucfirst($name);
        $lineSep = $augCode['lineSeparator'];
        $s = "public {$type} get{$nameCapitalized}() {{$lineSep}";
        $s .= "    return $name;$lineSep";
        $s .= "}";
        return $context->newContent($s);
    }
    
    public static function intentionalBlock($augCode, $context) {
        $augCodeWithEndMarkers = array_filter(
            $context->fileAugCodes['augmentingCodes'], function($v, $i) use(&$augCode, &$context) {
            return $i > $context->augCodeIndex &&
                $v['nestedLevelNumber'] == $augCode['nestedLevelNumber'] &&
                $v['hasNestedLevelEndMarker'];
        }, ARRAY_FILTER_USE_BOTH);
        assert($augCodeWithEndMarkers);
        $endingAugCode = array_values($augCodeWithEndMarkers)[0];
        $startGenCode = $context->newGenCode();
        $startGenCode->id = $augCode['id'];
        $startGenCode->indent = "  ";
        $startGenCode->contentParts[] = $context->newContent("{");
        $endGenCode = $context->newGenCode();
        $endGenCode->id = $endingAugCode['id'];
        $endGenCode->indent = "  ";
        $endGenCode->contentParts[] = $context->newContent("}");
        return [ $startGenCode, $endGenCode ];
    }
    
    public static function testSettingAllGenCodeProps($augCode, $context) {
        $genCode = $context->newGenCode();
        $genCode->id = $augCode['id'];
        $genCode->indent = "  ";
        $genCode->replaceAugCodeDirectives = TRUE;
        $genCode->replaceGenCodeDirectives = TRUE;
        $genCode->disableAutoIndent = TRUE;
        $genCode->contentParts[] = $context->newContent("//TODO", TRUE);
    
        $nextAugCode = $context->fileAugCodes['augmentingCodes'][$context->augCodeIndex + 1];
        assert(!!$nextAugCode);
        $nextGenCode = $context->newGenCode();
        $nextGenCode->id = $nextAugCode['id'];
        $nextGenCode->skipped = TRUE;
        $nextGenCode->contentParts[] = $context->newContent("");
        return [ $genCode, $nextGenCode ];
    }

    public static function testNegativeIdBypass($augCode, $context) {
        $g = $context->newGenCode();
        $g->id = -1;
        $g->contentParts[] = $context->newContent('test');
        return [ $g ];
    }

    public static function testHeader($augCode, $context) {
        assert('//:GS:' == $context->header['genCodeStartDirective']);
        assert('//:GE:' == $context->header['genCodeEndDirective']);
        assert('//:STR:' == $context->header['embeddedStringDirective']);
        assert('//:JSON:' == $context->header['embeddedJsonDirective']);
        assert('//:SS:' == $context->header['skipCodeStartDirective']);
        assert('//:SE:' == $context->header['skipCodeEndDirective']);
        assert('//:AUG_CODE:' == $context->header['augCodeDirective']);
        assert('//:GG:' == $context->header['inlineGenCodeDirective']);
        assert('[' == $context->header['nestedLevelStartMarker']);
        assert(']' == $context->header['nestedLevelEndMarker']);
        return '';
    }
}