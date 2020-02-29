package com.aaronicsubstances.programmer.companion.java;

import com.aaronicsubstances.programmer.companion.ParserSupport;

/**
 * JavaParser
 */
public class JavaParser {
    private final JavaSourceCodeWrapper inputSource;
    private final JavaLexer lexer;
    private final ParserSupport parserSupport;

    public JavaParser(JavaSourceCodeWrapper inputSource) {
        this.inputSource = inputSource;
        this.lexer = new JavaLexer();
        this.parserSupport = new ParserSupport(inputSource, lexer);
    }
}