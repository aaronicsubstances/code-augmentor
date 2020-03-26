package com.aaronicsubstances.code.augmentor.parsing.java;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.parsing.peg.extras.IndexRange;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.PegToken;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.StackEnabledParser;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.StackEnabledParsingContext;

public class JavaPegParser extends StackEnabledParser {
    
    public JavaPegParser(String input) {
        super(new StackEnabledParsingContext(input));
    }

    public List<PegToken> Parse() {
        EntireInput();
        return getTokenList();
    }

    void EntireInput() {
        final String ruleName = "EntireInput";
        runRule(ruleName, () -> {
            ZeroOrMore(() -> TopLevelObject());
            EOI();
        });
    }

    void TopLevelObject() {
        final String ruleName = "TopLevelObject";
        runRule(ruleName, () -> {
            FirstOf(
                () -> DsComment(true), 
                () -> SsComment(true), 
                () -> NonNewlineWhitespace(true),
                () -> Newline(true), 
                () -> StringExpression(), 
                () -> PackageStatement(), 
                () -> ImportStatement(),
                () -> IdOrNumOrKeyword(), 
                () -> OtherToken()
            );
        });
    }

    void DsComment(boolean significant) {
        runRule("DsComment", true, () -> {
            Str("//");
            ZeroOrMore(() -> NoneOf("\r\n"));
        });
        if (significant) {
            push(new PegToken(PegToken.TYPE_DS_COMMENT, matchRange()));
        }
    }

    void SsComment(boolean significant) {
        runRule("SsComment", true, () -> {
            Str("/*");
            ZeroOrMore(() -> SsCommentContent());
            Str("*/");
        });
        if (significant) {
            push(new PegToken(PegToken.TYPE_SS_COMMENT, matchRange()));
        }
    }

    void NonNewlineWhitespace(boolean significant) {
        runRule("NonNewlineWhitespace", true, () -> {
            OneOrMore(() -> AnyOf(" \t\f"));
        });
        if (significant) {
            push(new PegToken(PegToken.TYPE_NON_NEWLINE_WS, matchRange()));
        }
    }

    void Newline(boolean significant) {
        runRule("Newline", true, () -> {
            FirstOf(
                () -> Str("\r\n"), 
                () -> MatchChar('\r'), 
                () -> MatchChar('\n')
            );
        });
        if (significant) {
            push(new PegToken(PegToken.TYPE_NEWLINE, matchRange()));
        }
    }

    void StringExpression() {
        runRule("StringExpression", () -> {
            runRule(null, () -> {
                MatchChar('"');
            });
            push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange()));
            
            runRule(null, () -> {
                StringContent();
            });
            push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange()));

            runRule(null, () -> {
                MatchChar('"');
            });
            push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange()));
        });
    }

    void ImportStatement() {
        List<IndexRange> importStatement = new ArrayList<>();
        runRule("ImportStatement", true, () -> {
            
            runRule(null, () -> {
                Keyword("import");
            });
            importStatement.add(matchRange());

            Opt(() -> {
                Separators();
                runRule(null, () -> {
                    Keyword("static");
                });
                importStatement.add(matchRange());
            });
            Separators();
            ImportContent(importStatement);
            Separators();
            MatchChar(';');
        });
        push(new PegToken(PegToken.TYPE_IMPORT, matchRange(), importStatement));
    }

    void PackageStatement() {
        runRule("PackageStatement", true, () -> {
            Keyword("package"); 
            Separators(); 
            PackageContent();
            Separators();
            MatchChar(';');
        });
        push(new PegToken(PegToken.TYPE_PACKAGE, matchRange()));
    }

    void IdOrNumOrKeyword() {
        runRule("IdOrNumOrKeyword", true, () -> {
            TestNot(() -> Keywords(false), () -> "NOT select few keywords");
            OneOrMore(() -> IdOrNumContent());
        });
        push(new PegToken(PegToken.TYPE_QUASI_ID, matchRange()));
    }

    void OtherToken() {
        runRule("OtherToken", true, () -> {
            TestNot(() -> Keywords(false), () -> "NOT any of select few keywords");
            TestNot(() -> FirstOf(
                () -> Str("/*"), 
                () -> MatchChar('"') 
            ), () -> "NOT any of " + escapeString("/*") + ", " + escapeChar('"'));
            AnyChar();
        });
        push(new PegToken(PegToken.TYPE_OTHER, matchRange()));
    }

    private void SsCommentContent() {
        runRule("SsCommentContent", () -> {
            TestNot(() -> Str("*/"), () -> "NOT " + escapeString("*/"));
            AnyChar();
        });
    }

    private void StringContent() {
        runRule("StringContent", true, () -> {
            ZeroOrMore(() -> {
                TestNot(() -> MatchChar('"'), () -> "NOT " + escapeChar('"'));
                FirstOf(() -> EscapedStringContent(), () -> LiteralStringContent());
            });
        });
    }

    private void EscapedStringContent() {
        runRule("EscapedStringContent", () -> {
            MatchChar('\\');
            NoneOf("\r\n");
        });
    }

    private void LiteralStringContent() {
        runRule("LiteralStringContent", () -> {
            NoneOf("\r\n");
        });
    }

    private void ImportContent(List<IndexRange> importStatement) {
        runRule("ImportContent", () -> {
            
            runRule(null, () -> {
                IdOrNum();
            });
            importStatement.add(matchRange());
    
            ZeroOrMore(() -> {
                Separators();
    
                runRule(null, () -> {
                    MatchChar('.');
                });
                IndexRange temp = matchRange();
    
                Separators();
    
                runRule(null, () -> {
                    IdOrNum();
                });
    
                importStatement.add(temp);
                importStatement.add(matchRange());
            });
            Opt(() -> {
                Separators();
    
                runRule(null, () -> {
                    MatchChar('.');
                });
                IndexRange temp = matchRange();
    
                Separators();
    
                runRule(null, () -> {
                    MatchChar('*');
                });
    
                importStatement.add(temp);
                importStatement.add(matchRange());
            });
        });
    }

    private void PackageContent() {
        runRule("PackageContent", () -> {
            IdOrNum();
            ZeroOrMore(() -> {
                Separators();
                MatchChar('.');
                Separators();
                IdOrNum();
            });
        });
    }

    private void Separators() {
        runRule("Separators", () -> {
            ZeroOrMore(() ->
                FirstOf(
                    () -> NonNewlineWhitespace(false),
                    () -> Newline(false), 
                    () -> DsComment(false), 
                    () -> SsComment(false)
                )
            );
        });
    }

    private void IdOrNum() {
        runRule("IdOrNum", () -> {
            TestNot(() -> Keywords(true), () -> "NOT any of select Keywords");
            OneOrMore(() -> IdOrNumContent());
        });
    }

    private void IdOrNumContent() {
        runRule("IdOrNumContent", () -> {
            Char(Character::isJavaIdentifierPart, "valid Java identifier character");
        });
    }

    private void Keywords(boolean validate) {
        runRule("Keywords", () -> {            
            // keep updated with all usages of Keyword() below.
            FirstOf(
                () -> Str("import"),
                () -> Str("package"),
                validate ? () -> Str("static") : null
            );
            TestNot(() -> IdOrNumContent(), () -> "NOT IdOrNumContent");
        });
    }

    private void Keyword(String s) {
        runRule("Keyword", () -> {
            Str(s);
            TestNot(() -> IdOrNumContent(), () -> "NOT IdOrNumContent");
        });
    }
}