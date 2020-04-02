package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.parsing.ParserInputSource;
import com.aaronicsubstances.code.augmentor.parsing.SourceMap;
import com.aaronicsubstances.code.augmentor.parsing.Token;
import com.aaronicsubstances.code.augmentor.parsing.TokenSupplier;
import com.aaronicsubstances.code.augmentor.parsing.peg.NoMatchException;
import com.aaronicsubstances.code.augmentor.parsing.peg.PositionInfo;
import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext.ErrorDescription;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.IndexRange;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.PegToken;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.UnicodeBmpNormalizer;

/**
 * Parses Kotlin source code into a limited set of tokens from which new lines and comments
 * can be extracted.
 */
public class KotlinParser implements TokenSupplier {    
    private final ParserInputSource inputSource;
    private final KotlinPegParser pegParser;

    public KotlinParser(String input) {
        int startIndex = 0;
        KotlinPegParser trialPegParser = new KotlinPegParser(input);
        try {
            trialPegParser.Shebang();
            List<PegToken> trialList = trialPegParser.getTokenList();
            PegToken shebangToken = trialList.get(0);
            startIndex = shebangToken.endPos;
        }
        catch (NoMatchException ignore) {}

        // use 1 stage transformation.
        ParserInputSource inputSource1 = new ParserInputSource(input, new SourceMap());
        String transformedInput1 = UnicodeBmpNormalizer.replaceSupplementaryCharacters(
            input, startIndex, input.length(), 
            codePoint -> {
                // Kotlin's definition of valid identifiers is a subset of Java's own,
                // and maps to Character.isLetter(), LETTER_NUMBER, underscore and Character.isDigit()
                if (codePoint == '_' || Character.isLetter(codePoint) || 
                        Character.getType(codePoint) == Character.LETTER_NUMBER) {
                    return '_';
                }
                else if (Character.isDigit(codePoint)) {
                    return '0';
                }
                return '=';
            },
            inputSource1.getSourceMap());

        inputSource = new ParserInputSource(transformedInput1);
        inputSource.setEmbeddedInputSource(inputSource1);

        pegParser = new KotlinPegParser(inputSource.getInput());
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
        for (int tIndex = 0; tIndex < tokenList.size(); tIndex++) {
            PegToken t = tokenList.get(tIndex);
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

            int[] temp = new int[]{ t.startPos };
            inputSource.fixInputCoordinates(temp);
            int originalStartPos = temp[0];

            temp[0] = t.endPos;
            inputSource.fixInputCoordinates(temp);
            int originalEndPos = temp[0];

            String text = originalInput.substring(originalStartPos, originalEndPos);
            Map<String, Object> value = null;
            if (t.importStatement != null) {
                if (value == null) {
                    value = new HashMap<>();
                }
                StringBuilder importStatementStr = new StringBuilder();
                for (IndexRange range : t.importStatement) {                    
                    temp = new int[]{ range.start };
                    inputSource.fixInputCoordinates(temp);
                    int originalRangeStart = temp[0];

                    temp[0] = range.end;
                    inputSource.fixInputCoordinates(temp);
                    int originalRangeEnd = temp[0];

                    String s = originalInput.substring(originalRangeStart, originalRangeEnd);
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

            if (prevEndPos == -1) {
                assert retToken.startPos == 0;
            }
            else {
                assert prevEndPos == retToken.startPos;
            }
            prevEndPos = retToken.endPos;
            int lineNumberInc = LexerSupport.calculateLineAndColumnNumbers(text, text.length())[0];
            lineNumber += lineNumberInc - 1;
        }

        // determine which whitespace tokens are mandatory.
        for (int tIndex = 0; tIndex < tokenList.size();) {
            // whitespace tokens are mandatory if they appear in between two tokens of types
            // id, keyword or number, and are NOT delimited with back ticks or semi-colons. 
            // PegToken types which match are quasi id, import or package.
            PegToken currRawToken = tokenList.get(tIndex);
            Token currProcessedToken = parseResults.get(tIndex);
            boolean wsDelimitingPossible = false;
            if (currRawToken.type == PegToken.TYPE_QUASI_ID &&
                    !currProcessedToken.text.endsWith("`")) {
                wsDelimitingPossible = true;
            }
            else if (currRawToken.type == PegToken.TYPE_IMPORT &&
                    !currProcessedToken.text.endsWith(";") &&
                    !currProcessedToken.text.endsWith("*")) {
                wsDelimitingPossible = true;
            }
            else if (currRawToken.type == PegToken.TYPE_PACKAGE &&
                    !currProcessedToken.text.endsWith(";")) {
                wsDelimitingPossible = true;
            }
            if (wsDelimitingPossible) {
                // scan ahead while only whitespace characters are seen.
                int scIndex = tIndex + 1;
                PegToken nextRawTokenOfInterest = null;
                while (scIndex < tokenList.size()) {
                    PegToken search = tokenList.get(scIndex);
                    if (search.type != PegToken.TYPE_NON_NEWLINE_WS &&
                            search.type != PegToken.TYPE_NEWLINE) {
                        nextRawTokenOfInterest = search;
                        break;
                    }
                    scIndex++;
                }
                // if we hit another quasi id, import or package.
                if (nextRawTokenOfInterest != null) {
                    if (nextRawTokenOfInterest.type == PegToken.TYPE_QUASI_ID ||
                            nextRawTokenOfInterest.type == PegToken.TYPE_PACKAGE ||
                            nextRawTokenOfInterest.type == PegToken.TYPE_IMPORT) {
                        Token processedToken = parseResults.get(scIndex - 1);
                        assert processedToken.type == Token.TYPE_NON_NEWLINE_WHITESPACE ||
                                processedToken.type == Token.TYPE_NEWLINE;
                        if (processedToken.value == null) {
                            processedToken.value = new HashMap<>();
                        }
                        processedToken.value.put(Token.VALUE_KEY_WS_REQD, true);
                        tIndex = scIndex;
                        continue;
                    }
                }
            }
            tIndex++;
        }

        return parseResults;
    }
}