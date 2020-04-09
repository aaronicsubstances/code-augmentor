package com.aaronicsubstances.code.augmentor.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SourceCodeTokenizer {
    private final List<String> genCodeStartDirectives;
    private final List<String> genCodeEndDirectives;
    private final List<String> embeddedStringDirectives;
    private final List<String> embeddedJsonDirectives;
    private final List<String> enableScanDirectives;
    private final List<String> disableScanDirectives;
    private final List<List<String>> augCodeDirectiveSets;

    public SourceCodeTokenizer(
            List<String> genCodeStartDirectives,
            List<String> genCodeEndDirectives,
            List<String> embeddedStringDirectives,
            List<String> embeddedJsonDirectives,
            List<String> enableScanDirectives, 
            List<String> disableScanDirectives,
            List<List<String>> augCodeDirectiveSets) {
        this.genCodeStartDirectives = genCodeStartDirectives;
        this.genCodeEndDirectives = genCodeEndDirectives;
        this.embeddedStringDirectives = embeddedStringDirectives;
        this.embeddedJsonDirectives = embeddedJsonDirectives;
        this.enableScanDirectives = enableScanDirectives;
        this.disableScanDirectives = disableScanDirectives;
        this.augCodeDirectiveSets = augCodeDirectiveSets;
    }

    public List<Token> tokenizeSource(String source) {
        List<String> splitSource = TaskUtils.splitIntoLines(source);
        List<Token> tokens = new ArrayList<>();
        int startPos = 0;
        for (int i = 0; i < splitSource.size(); i+=2) {
            String line = splitSource.get(i);
            String terminator = splitSource.get(i + 1);
            String lineWithoutIndent = line.trim();
            Token t = null;
            if (lineWithoutIndent.isEmpty()) {
                t = new Token(Token.TYPE_BLANK);
            }
            if (t == null) {
                // Pick longest token out of all candidate
                // directive tokens.
                List<Token> candidateTokens = new ArrayList<>();
                for (String d : genCodeStartDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_GEN_CODE_START, d, line));
                        break;
                    }
                }
                
                for (String d : genCodeEndDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_GEN_CODE_END, d, line));
                        break;
                    }
                }
                
                for (String d : embeddedStringDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_EMB_STRING, d, line));
                        break;
                    }
                }
                
                if (embeddedJsonDirectives != null) {
                    for (String d : embeddedJsonDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_EMB_JSON, d, line));
                            break;
                        }
                    }
                }
                
                if (enableScanDirectives != null) {
                    for (String d : enableScanDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_ENABLE_SCAN, d, line));
                            break;
                        }
                    }
                }
                
                if (disableScanDirectives != null) {
                    for (String d : disableScanDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_DISABLE_SCAN, d, line));
                            break;
                        }
                    }
                }
                
                outer1: for (int j = 0; j < augCodeDirectiveSets.size(); j++) {
                    List<String> augCodeDirectives = augCodeDirectiveSets.get(j);
                    for (String d : augCodeDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            Token c = createToken(Token.DIRECTIVE_TYPE_AUG_CODE, d, line);
                            c.augCodeSpecIndex = j;
                            candidateTokens.add(c);
                            break outer1;
                        }
                    }
                }

                Optional<Token> tOpt = candidateTokens.stream().max(
                    (x, y) -> new Integer(x.text.length()).compareTo(y.text.length()));
                if (tOpt.isPresent()) {
                    t = tOpt.get();
                }
            }
            if (t == null) {
                // token must be of some other type.
                t = new Token(Token.TYPE_OTHER);
            }
            t.text = line;
            if (terminator != null) {
                t.text += terminator;
                t.newline = terminator;
            }
            t.startPos = startPos;
            t.endPos = startPos + t.text.length();
            // collapse (line, terminator) pair from split source
            // to single index.
            t.index = i / 2;
            t.lineNumber = t.index + 1;
            
            tokens.add(t);

            startPos = t.endPos;
        }
        return tokens;
    }

    private static Token createToken(int directiveType, String directiveMarker, String line) {
        Token t = new Token(directiveType);
        t.directiveMarker = directiveMarker;
        int dIndex = line.indexOf(directiveMarker);
        t.indent = line.substring(0, dIndex);
        t.directiveContent = line.substring(dIndex + directiveMarker.length());
        // set text even though we don't have newline, so search 
        // for directive token with max length can work.
        t.text = line;
        return t;
    }
}