package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aaronicsubstances.code.augmentor.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.parsing.ParserException;
import com.aaronicsubstances.code.augmentor.parsing.ParserInputSource;
import com.aaronicsubstances.code.augmentor.parsing.PegToken;
import com.aaronicsubstances.code.augmentor.parsing.Token;
import com.aaronicsubstances.code.augmentor.parsing.TokenSupplier;

import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.IndexRange;
//import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

/**
 * Parses Kotlin source code into a limited set of tokens from which new lines and comments
 * can be extracted.
 */
public class KotlinParser implements TokenSupplier {    
    private final ParserInputSource inputSource;

    public KotlinParser(String input) {
        inputSource = new ParserInputSource(input);
    }

    @Override
    public ParserInputSource getInputSource() {
        return inputSource;
    }

	@Override
    public List<Token> parse() {
        KotlinPegParser parser = Parboiled.createParser(KotlinPegParser.class);
        Rule startRule = parser.Parse();

        String originalInput = inputSource.getInput();
        StringBuilder parserFriendlyInput = new StringBuilder();
        for (int i = 0; i < originalInput.length(); i++) {
            char ch = originalInput.charAt(i);
            if (LexerSupport.isValidC89IdentifierChar(ch, false)) {
                parserFriendlyInput.append(ch);
            }
            else {
                // Kotlin's definition of valid identifiers is a subset of Java's own,
                // and maps to Character.isLetter(), LETTER_NUMBER, underscore and Character.isDigit()
                if (ch == '_' || Character.isLetter(ch) || 
                        Character.getType(ch) == Character.LETTER_NUMBER) {
                    parserFriendlyInput.append('_');
                }
                else if (Character.isDigit(ch)) {
                    parserFriendlyInput.append('0');

                }
                else {
                    parserFriendlyInput.append(ch);
                }
            }
        }

        ParsingResult<PegToken> result = new ReportingParseRunner<PegToken>(startRule).run(
            parserFriendlyInput.toString());
        if (result.hasErrors()) {
            String errorMsg = ErrorUtils.printParseErrors(result);
            throw new ParserException(errorMsg);
        }
        //System.out.println("\nParse Tree:\n" + ParseTreeUtils.printNodeTree(result) + '\n');

        List<PegToken> tokenList = new ArrayList<>();
        for (PegToken t : result.valueStack) {
            // reverse stack
            tokenList.add(0, t);
        }

        // Convert tokens.
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