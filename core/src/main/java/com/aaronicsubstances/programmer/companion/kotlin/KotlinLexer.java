package com.aaronicsubstances.programmer.companion.kotlin;

import com.aaronicsubstances.programmer.companion.LexerSupport;
import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.java.JavaLexer;

/**
 * Lexer for Kotlin programmingn language.
 */
public class KotlinLexer extends JavaLexer {
    public static final int TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER = 57;

    @Override
    protected Token fetchNextToken(ParserInputSource inputSource) {
        // deal with shebang
        if (inputSource.getPosition() == 0 && inputSource.lookahead(0) == '#') {
            StringBuilder shebang = new StringBuilder();
            int ch;
            while ((ch = inputSource.lookahead(0)) != LexerSupport.EOF) {
                if (LexerSupport.isNewLine(ch)) {
                    break;
                }
                shebang.appendCodePoint(ch);
                inputSource.consume(1);
            }            
            Token shebangToken = new Token(TOKEN_TYPE_OTHER, 
                shebang.toString(), 0, inputSource.getPosition(), 1, 1);
            return shebangToken;
        }
        if (inputSource.lookahead(0) == '"' &&
                inputSource.lookahead(1) == '"' &&
                inputSource.lookahead(2) == '"') {
            return consumeToken(inputSource, TOKEN_TYPE_MULTI_LINE_STRING_DELIMITER, 
                "\"\"\"", 3);
        }
        return super.fetchNextToken(inputSource);
    }

    @Override
    public Token consumeSingleLineString(ParserInputSource inputSource) {
        throw new UnsupportedOperationException();
    }

    public Token consumeMultipleLineString(ParserInputSource inputSource) {
        throw new UnsupportedOperationException();
    }
}