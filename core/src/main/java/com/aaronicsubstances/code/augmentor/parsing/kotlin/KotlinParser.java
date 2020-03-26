package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.parsing.ParserInputSource;
import com.aaronicsubstances.code.augmentor.parsing.Token;
import com.aaronicsubstances.code.augmentor.parsing.TokenSupplier;
import com.aaronicsubstances.code.augmentor.parsing.peg.NoMatchException;
import com.aaronicsubstances.code.augmentor.parsing.peg.PositionInfo;
import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext.ErrorDescription;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.IndexRange;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.PegToken;

/**
 * Parses Kotlin source code into a limited set of tokens from which new lines and comments
 * can be extracted.
 */
public class KotlinParser implements TokenSupplier {    
    private final ParserInputSource inputSource;
    private final KotlinPegParser pegParser;

    public KotlinParser(String input) {
        inputSource = new ParserInputSource(input);
        String transformedInput = inputSource.getInput();
        pegParser = new KotlinPegParser(transformedInput);
    }

    @Override
    public ParserInputSource getInputSource() {
        return inputSource;
    }

    public void setTraceLog(Consumer<String> traceLog) {
        pegParser.setTraceLog(traceLog);
    }

    public void setVerbose(boolean verbose) {
        pegParser.setVerbose(verbose);
    }

	@Override
    public List<Token> parse() {
        List<PegToken> tokenList;
        try {
            tokenList = pegParser.Parse();
        }
        catch (NoMatchException ex) {
            ErrorDescription errorDesc = pegParser.getParsingContext().getErrorDescription();
            PositionInfo errorLineInfo = inputSource.createErrorLineInfo(errorDesc.errorPosition);
            String errorMsg = "Expected: " +
                    errorDesc.expectations.stream().collect(Collectors.joining(", ")) + 
                    " instead of '" + errorLineInfo.getPositionChar() + '\'';
            throw inputSource.createAbortException(errorLineInfo, errorMsg);
        }

        // Convert tokens.
        String originalInput = inputSource.getInput();
        List<Token> parseResults = new ArrayList<>();
        int prevEndPos = -1;
        int lineNumber = 1;
        for (PegToken t : tokenList) {
            int type;
            switch (t.type) {
                case PegToken.TYPE_DS_COMMENT:
                    type = Token.TYPE_SINGLE_LINE_COMMENT;
                    break;
                case PegToken.TYPE_SS_COMMENT:
                    type = Token.TYPE_MULTI_LINE_COMMENT;
                    break;
                case PegToken.TYPE_NON_NEWLINE_WS:
                    type = Token.TYPE_NON_NEWLINE_WHITESPACE;
                    break;
                case PegToken.TYPE_NEWLINE:
                    type = Token.TYPE_NEWLINE;
                    break;
                case PegToken.TYPE_PACKAGE:
                    type = Token.TYPE_PACKAGE_STATEMENT;
                    break;
                case PegToken.TYPE_IMPORT:
                    type = Token.TYPE_IMPORT_STATEMENT;
                    break;
                case PegToken.TYPE_LITERAL_STRING_CONTENT:
                    type = Token.TYPE_LITERAL_STRING_CONTENT;
                    break;
                case PegToken.TYPE_SHEBANG:
                    type = Token.TYPE_SHEBANG;
                    break;
                case PegToken.TYPE_STRING_DELIMITER:
                case PegToken.TYPE_TRIPLE_QUOTED_STRING_DELIMITER:
                case PegToken.TYPE_STRING_TEMPLATE_START:
                case PegToken.TYPE_STRING_TEMPLATE_END:
                case PegToken.TYPE_BRACED_BLOCK_START:
                case PegToken.TYPE_BRACED_BLOCK_END:
                case PegToken.TYPE_QUASI_ID:
                case PegToken.TYPE_OTHER:
                    type = Token.TYPE_OTHER;
                    break;
                default:
                    inputSource.setPosition(t.startPos);
                    throw inputSource.createAbortException("Unexpected token type: " +
                        t.type, null);
            }

            int originalStartPos = t.startPos;
            int originalEndPos = t.endPos;

            String text = originalInput.substring(originalStartPos, originalEndPos);
            Map<String, Object> value = null;
            if (t.importStatement != null) {
                value = new HashMap<>();
                StringBuilder importStatementStr = new StringBuilder();
                for (IndexRange range : t.importStatement) {
                    String s = originalInput.substring(range.start, range.end);
                    // deal with backticks.
                    if (s.startsWith("`")) {
                        assert s.charAt(s.length() - 1) == '`';
                        s = s.substring(1, s.length() - 1);
                    }
                    if ("as".equals(s)) {
                        importStatementStr.append(' ');
                    }
                    importStatementStr.append(s);
                    if ("import".equals(s) || "as".equals(s)) {
                        importStatementStr.append(' ');
                    }
                }
                value.put(Token.VALUE_KEY_IMPORT_STATEMENT, importStatementStr.toString());
            }
            Token retToken = new Token(type, text, originalStartPos, originalEndPos, lineNumber);
            retToken.value = value;
            parseResults.add(retToken);

            if (prevEndPos != -1) {
                assert prevEndPos == retToken.startPos;
            }
            prevEndPos = retToken.endPos;

            int lineNumberInc = LexerSupport.calculateLineAndColumnNumbers(text, text.length())[0];
            lineNumber += lineNumberInc - 1;
        }
        return parseResults;
    }
}