package com.aaronicsubstances.programmer.companion;

import java.util.List;

/**
 * Interface for code lexers.
 */
public interface Lexer {

    List<Token> next(ParserInputSource inputSource);
	String getTokenName(int tokenType);
	String describeToken(Token token);
}