package com.aaronicsubstances.programmer.companion;

/**
 * KotlinLexer
 */
public class KotlinLexer extends JavaLexer {
    protected static final int MODE_MULTI_LINE_STRING = 8;
    
    private final boolean nested;
    private boolean noMoreTokens = false;

    private KotlinLexer nestedLexer;

    public KotlinLexer(String sourceCode) {
        this(sourceCode, false);
    }

    private KotlinLexer(String sourceCode, boolean nested) {
        super(sourceCode);
        this.nested = nested;
    }
	
	@Override
	public Token next() {
        if (nestedLexer != null) {
            Token temp = pullNestedLexerToken();
            if (temp != null) {
                return temp;
            }
        }
        if (noMoreTokens) { // not used by outermost lexer
            return null;
        }
        if (savedToken != null) {
            Token temp = savedToken;
            savedToken = null;
            return temp;
        }
        if (position >= sourceCode.length()) {
            return null;
        }

        // Check for shebang
        if (position == 0 && sourceCode.startsWith("#!")) {
            StringBuilder shebang = new StringBuilder("#!");
            int i;
            for (i = 2; i < sourceCode.length(); i++) {
                char lookup = sourceCode.charAt(i);
                if (lookup == '\r' || lookup == '\n') {
                    break;
                }
                shebang.append(lookup);
            }
            return consumeToken(Token.TOKEN_TYPE_OTHER, shebang.toString(), i);
        }
        Token nextToken = null;
		switch (mode) {
			case MODE_SINGLE_LINE_STRING:
				nextToken = processSingleLineString();
				break;
			case MODE_MULTI_LINE_STRING:
				nextToken = processMultiLineString();
				break;
		}
		if (nextToken == null) {
			nextToken = determineNextToken();
		}
		return nextToken;
    }

    private Token determineNextToken() {
        char lookup = sourceCode.charAt(position);
        if (lookup == '"') {
            int nextChar = -1, char3 = -1;
            if (position + 2 < sourceCode.length()) {
                nextChar = sourceCode.charAt(position + 1);
                char3 = sourceCode.charAt(position + 2);
            }
            if (nextChar == '"' && char3 == '"') {
                mode = MODE_MULTI_LINE_STRING;
                return consumeToken(Token.TOKEN_TYPE_MULTI_LINE_STRING_START, "\"\"\"", 3);
            }
            else {
                mode = MODE_SINGLE_LINE_STRING;
                return consumeToken(Token.TOKEN_TYPE_SINGLE_LINE_STRING_START, "\"", 1);
            }
        }
        else if (nested && lookup == '}') { 
            noMoreTokens = true;
            return consumeToken(Token.TOKEN_TYPE_OTHER, "}", 1);
        }
        else {
            return super.next();
        }
    }
	
	private Token processSingleLineString() {
        char lookup = sourceCode.charAt(position);
		if (lookup == '\"') {
			mode = MODE_NONE;
			return consumeToken(Token.TOKEN_TYPE_SINGLE_LINE_STRING_END, "\"", 1);
        }
        int nextChar = -1;
        if (position + 1 < sourceCode.length()) {
            nextChar = sourceCode.charAt(position + 1);
        }
        if (lookup == '$' && nextChar == '{') {
            Token token = consumeToken(Token.TOKEN_TYPE_OTHER, "${", 2);
            setUpNestedLexer();
            return token;
        }
		boolean escaped = false;
		StringBuilder content = new StringBuilder();
        int i;
        for (i = position; i < sourceCode.length(); i++) {
			lookup = sourceCode.charAt(i);
			if (lookup == '\r' || lookup == '\n') {
				mode = MODE_NONE;
				break;
            }
            nextChar = -1;
            if (i + 1 < sourceCode.length()) {
                nextChar = sourceCode.charAt(i + 1);
            }
            if (!escaped) {
                if (lookup == '"') {
                    break;
                }
                if (lookup == '$' && nextChar == '{') {
                    break;
                }
                if (lookup == '\\') {
                    escaped = true;
                }
            }
            else {
                escaped = false;
            }
            content.append(lookup);
		}
		// abnormal, but check for newline in string to avoid
		// lexer misinterpreting subsequent characters.
		if (content.length() == 0) {
			assert mode == MODE_NONE;
			return null;
		}
		return consumeToken(Token.TOKEN_TYPE_OTHER, content.toString(), i - position);
	}

    private Token processMultiLineString() {
		char lookup = sourceCode.charAt(position);
        int nextChar = -1, char3 = -1;
        if (position + 1 < sourceCode.length()) {
            nextChar = sourceCode.charAt(position + 1);
            if (position + 2 < sourceCode.length()) {
                char3 = sourceCode.charAt(position + 2);
            }
        }
		if (lookup == '\"' && nextChar == '"' && char3 == '"') {
			mode = MODE_NONE;
			return consumeToken(Token.TOKEN_TYPE_MULTI_LINE_STRING_END, "\"\"\"", 1);
        }
        if (lookup =='$' && nextChar == '{') {
            Token token = consumeToken(Token.TOKEN_TYPE_OTHER, "${", 2);
            setUpNestedLexer();
            return token;
        }
		StringBuilder content = new StringBuilder();
        int i;
        for (i = position; i < sourceCode.length(); i++) {
            lookup = sourceCode.charAt(i);
            nextChar = -1;
            char3 = -1;
            if (i + 1 < sourceCode.length()) {
                nextChar = sourceCode.charAt(i + 1);
                if (i + 2 < sourceCode.length()) {
                    char3 = sourceCode.charAt(i + 2);
                }
            }
			if (lookup == '\"' && nextChar == '"' && char3 == '"') {
				break;
            }
            if (lookup == '$' && nextChar == '{') {
                break;
            }
            content.append(lookup);
		}
		assert content.length() > 0;
		return consumeToken(Token.TOKEN_TYPE_OTHER, content.toString(), i - position);
    }

    private void setUpNestedLexer() {
        nestedLexer = new KotlinLexer(sourceCode, true);
        nestedLexer.position = position;
        nestedLexer.lineNumber = lineNumber;
        nestedLexer.columnNumber = columnNumber;
    }

    private Token pullNestedLexerToken() {
        if (nested) {
            Token nextToken = nestedLexer.next();
            if (nextToken == null) {
                // pull down nested lexer.
                position = nestedLexer.position;
                nestedLexer = null;
            }
            return nextToken;
        }

        // try and collapse tokens, except for newlines.
        StringBuilder content = null; 
        int lineNumber = 0, columnNumber = 0, startPos = 0, endPos = 0;
        while (true) {
            Token token = nestedLexer.next();
            if (token == null) {
                break;
            }
            if (token.type == Token.TOKEN_TYPE_NEWLINE) {
                if (content == null) {
                    return token;
                }
                else {
                    break;
                }
            }
            if (content == null) {
                // use first token to identify start of token.
                content = new StringBuilder();
                startPos = token.startPos;
                lineNumber = token.lineNumber;
                columnNumber = token.columnNumber;
            }
            // use last token to identify end of token.
            endPos = token.endPos;
            if (token.text != null) {
                content.append(token.text);
            }
        }
        if (content == null) {
            position = nestedLexer.position;
            nestedLexer = null;
            return null;
        }
        return new Token(Token.TOKEN_TYPE_OTHER, 
            content.length() == 0 ? null : content.toString(),
            startPos, endPos, lineNumber, columnNumber);
    }
}