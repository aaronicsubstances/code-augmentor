package com.aaronicsubstances.code.augmentor.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SourceCodeTokenizer {
    private final List<String> genCodeStartDirectives;
    private final List<String> genCodeEndDirectives;
    private final List<String> embeddedStringDirectives;
    private final List<String> embeddedJsonDirectives;
    private final List<String> skipCodeStartDirectives;
    private final List<String> skipCodeEndDirectives;
    private final List<List<String>> augCodeDirectiveSets;
    private final List<String> inlineGenCodeDirectives;
    private final List<String> nestedLevelStartMarkers;
    private final List<String> nestedLevelEndMarkers;

    public SourceCodeTokenizer(
            List<String> genCodeStartDirectives,
            List<String> genCodeEndDirectives,
            List<String> embeddedStringDirectives,
            List<String> embeddedJsonDirectives,
            List<String> skipCodeStartDirectives, 
            List<String> skipCodeEndDirectives,
            List<List<String>> augCodeDirectiveSets, 
            List<String> inlineGenCodeDirectives, 
            List<String> nestedLevelStartMarkers, 
            List<String> nestedLevelEndMarkers) {
        this.genCodeStartDirectives = genCodeStartDirectives;
        this.genCodeEndDirectives = genCodeEndDirectives;
        this.embeddedStringDirectives = embeddedStringDirectives;
        this.embeddedJsonDirectives = embeddedJsonDirectives;
        this.skipCodeStartDirectives = skipCodeStartDirectives;
        this.skipCodeEndDirectives = skipCodeEndDirectives;
        this.augCodeDirectiveSets = augCodeDirectiveSets;
        this.inlineGenCodeDirectives = inlineGenCodeDirectives;
        this.nestedLevelStartMarkers = nestedLevelStartMarkers;
        this.nestedLevelEndMarkers = nestedLevelEndMarkers;
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
                        Token c = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_START, d, line);
                        c.isGeneratedCodeMarker = true;
                        candidateTokens.add(c);
                    }
                }
                
                for (String d : genCodeEndDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        Token c = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_END, d, line);
                        c.isGeneratedCodeMarker = true;
                        candidateTokens.add(c);
                    }
                }
                
                if (inlineGenCodeDirectives != null) {
                    for (String d : inlineGenCodeDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            Token c = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_START, d, line);
                            c.isGeneratedCodeMarker = true;
                            c.isInlineGeneratedCodeMarker = true;
                            candidateTokens.add(c);
                        }
                    }
                }
                
                for (String d : embeddedStringDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_EMB_STRING, d, line));
                    }
                }
                
                for (String d : embeddedJsonDirectives) {
                    if (lineWithoutIndent.startsWith(d)) {
                        candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_EMB_JSON, d, line));
                    }
                }
                
                if (skipCodeStartDirectives != null) {
                    for (String d : skipCodeStartDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_START, d, line));
                        }
                    }
                }
                
                if (skipCodeEndDirectives != null) {
                    for (String d : skipCodeEndDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            candidateTokens.add(createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_END, d, line));
                        }
                    }
                }
                
                for (int j = 0; j < augCodeDirectiveSets.size(); j++) {
                    List<String> augCodeDirectives = augCodeDirectiveSets.get(j);
                    for (String d : augCodeDirectives) {
                        if (lineWithoutIndent.startsWith(d)) {
                            Token c = createToken(Token.DIRECTIVE_TYPE_AUG_CODE, d, line);
                            c.augCodeSpecIndex = j;
                            candidateTokens.add(c);
                        }
                    }
                }

                Optional<Token> tOpt = candidateTokens.stream().max(
                    (x, y) -> Integer.compare(x.directiveMarker.length(), y.directiveMarker.length()));
                if (tOpt.isPresent()) {
                    t = tOpt.get();
                    if (t.type == Token.DIRECTIVE_TYPE_AUG_CODE) {
                        String originalDirectiveContent = t.directiveContent;
                        String longestMarker = null;
                        // identify longest marker if any
                        if (nestedLevelStartMarkers != null) {
                            for (String m : nestedLevelStartMarkers) {
                                if (t.directiveContent.startsWith(m)) {
                                    if (longestMarker == null ||
                                            m.length() > longestMarker.length()) {
                                        // reset end marker
                                        t.nestedLevelEndMarker = null;
                                        longestMarker = t.nestedLevelStartMarker = m;
                                        t.directiveContent = originalDirectiveContent.substring(m.length());
                                    }
                                }
                            }
                        }
                        if (nestedLevelEndMarkers != null) {
                            for (String m : nestedLevelEndMarkers) {
                                if (t.directiveContent.startsWith(m)) {
                                    if (longestMarker == null ||
                                            m.length() > longestMarker.length()) {
                                        // reset start marker
                                        t.nestedLevelStartMarker = null;
                                        longestMarker = t.nestedLevelEndMarker = m;
                                        t.directiveContent = originalDirectiveContent.substring(m.length());
                                    }
                                }
                            }
                        }
                    }
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
        return t;
    }
}