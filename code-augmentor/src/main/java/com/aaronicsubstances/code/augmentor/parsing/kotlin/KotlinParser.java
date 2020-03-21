package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.parsing.ParserInputSource;
import com.aaronicsubstances.code.augmentor.parsing.Token;
import com.aaronicsubstances.code.augmentor.parsing.TokenSupplier;

/**
 * Parses Kotlin source code into a limited set of tokens from which new lines and comments
 * can be extracted.
 */
public class KotlinParser implements TokenSupplier {
    public static final int TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER = 57;
    public static final int TOKEN_TYPE_STRING_TEMPLATE_START = 60;
    public static final int TOKEN_TYPE_STRING_TEMPLATE_END = 61;
    
    private ParserInputSource inputSource;

    public KotlinParser(String input) {
    }

    @Override
    public ParserInputSource getInputSource() {
        return inputSource;
    }

	@Override
    public List<Token> parse() {
        List<Token> parseResults = new ArrayList<>();
        return parseResults;
    }
}