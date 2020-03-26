package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.parsing.peg.extras.IndexRange;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.PegToken;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.StackEnabledParser;
import com.aaronicsubstances.code.augmentor.parsing.peg.extras.StackEnabledParsingContext;

public class KotlinPegParser extends StackEnabledParser {

    public KotlinPegParser(String input) {
        super(new StackEnabledParsingContext(input));
    }

    public List<PegToken> Parse() {
        EntireInput();
        return getTokenList();
    }

    void EntireInput() {
        runRule("EntireInput", () -> {
            Opt(() -> Shebang());
            ZeroOrMore(() -> TopLevelObject());
            EOI();
        });
    }

    void Shebang() {
        runRule("Shebang", true, () -> {
            markRuleStart();
            Str("#!");
            ZeroOrMore(() -> NoneOf("\r\n"));
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_SHEBANG, matchRange()));
        });
    }

    void TopLevelObject() {
        runRule("TopLevelObject", () -> {
            FirstOf(
                () -> BracedBlock(), 
                () -> DsComment(true), 
                () -> SsComment(true), 
                () -> NonNewlineWhitespace(true), 
                () -> Newline(true),
                () -> TripleQuotedStringExpression(), 
                () -> SingleQuotedStringExpression(), 
                () -> PackageStatement(), 
                () -> ImportStatement(),
                () -> IdOrNumOrKeyword(), 
                () -> OtherToken()
            );
        });
    }

    void BracedBlock() {
        runRule("BracedBlock", true, () -> {
            markRuleStart();
            MatchChar('{');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_BRACED_BLOCK_START, matchRange()));

            ZeroOrMore(() -> TopLevelObject());
            
            markRuleStart();
            MatchChar('}');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_BRACED_BLOCK_END, matchRange()));
        });
    }

    void DsComment(boolean significant) {
        runRule("DsComment", true, () -> {
            markRuleStart();
            Str("//");
            ZeroOrMore(() -> NoneOf("\r\n"));
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_DS_COMMENT, matchRange()));
            }
        });
    }

    void SsComment(boolean significant) {
        runRule("SsComment", true, () -> {
            markRuleStart();
            Str("/*");
            ZeroOrMore(() -> SsCommentContent());
            Str("*/");
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_SS_COMMENT, matchRange()));
            }
        });
    }

    void NonNewlineWhitespace(boolean significant) {
        runRule("NonNewlineWhitespace", true, () -> {
            markRuleStart();
            OneOrMore(() -> AnyOf(" \t\f"));
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_NON_NEWLINE_WS, matchRange()));
            }
        });
    }

    void Newline(boolean significant) {
        runRule("Newline", true, () -> {
            markRuleStart();
            FirstOf(
                () -> Str("\r\n"), 
                () -> MatchChar('\r'), 
                () -> MatchChar('\n')
            );
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_NEWLINE, matchRange()));
            }
        });
    }

    void SingleQuotedStringExpression() {
        runRule("SingleQuotedStringExpression", true, () -> {
            markRuleStart();
            MatchChar('"');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange()));
            
            SingleQuotedStringContent();
            
            markRuleStart();
            MatchChar('"');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange()));
        });
    }

    void TripleQuotedStringExpression() {
        runRule("TripleQuotedStringExpression", true, () -> {
            markRuleStart();
            Str("\"\"\"");
            markRuleEnd(); 
            push(new PegToken(PegToken.TYPE_TRIPLE_QUOTED_STRING_DELIMITER, matchRange()));
            
            TripleQuotedStringContent();
            
            markRuleStart();
            Str("\"\"\"");
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_TRIPLE_QUOTED_STRING_DELIMITER, matchRange()));
        });
    }

    void PackageStatement() {
        runRule("PackageStatement", true, () -> {
            markRuleStart();
            Keyword("package"); 
            Separators();
            PackageContent();
            Opt(() -> {
                Separators();
                MatchChar(';');
            });
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_PACKAGE, matchRange()));
        });
    }

    void ImportStatement() {
        runRule("ImportStatement", true, () -> {
            List<IndexRange> importStatement = new ArrayList<>();

            markRuleStart();

            markRuleStart();
            Keyword("import");
            markRuleEnd();
            importStatement.add(matchRange());
            
            Separators();
            ImportContent(importStatement);
            Opt(() -> {
                Separators();

                markRuleStart();
                Keyword("as");
                markRuleEnd();
                IndexRange temp = matchRange();
                
                Separators();

                markRuleStart();
                IdOrNum();
                markRuleEnd();
                
                importStatement.add(temp);
                importStatement.add(matchRange());
            });            
            Opt(() -> {
                Separators();
                MatchChar(';');
            });

            markRuleEnd();
            push(new PegToken(PegToken.TYPE_IMPORT, matchRange(), importStatement));
        });
    }

    void IdOrNumOrKeyword() {
        runRule("IdOrNumOrKeyword", true, () -> {
            markRuleStart();
            TestNot(() -> Keywords(false), () -> "NOT any of select few keywords");
            KotlinIdOrNumContent();
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_QUASI_ID, matchRange()));
        });
    }

    void OtherToken() {
        runRule("OtherToken", true, () -> {
            markRuleStart();
            TestNot(() -> Keywords(false), () -> "NOT any of select few keywords");
            TestNot(() -> FirstOf(
                () -> Str("/*"), 
                () -> Str("\"\"\""), 
                () -> MatchChar('"'), 
                () -> MatchChar('`'), 
                () -> MatchChar('{'), 
                () -> MatchChar('}')
            ), () -> "NOT any of " + escapeString("/*") + ", " + escapeString("\"\"\"") + ", " +
                escapeChar('"') + ", " + escapeChar('`') + ", " + escapeChar('{') + ", " +
                escapeChar('}'));
            AnyChar();
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_OTHER, matchRange()));
        });
    }

    private void SsCommentContent() {
        runRule("SsCommentContent", () -> {            
            FirstOf(
                () -> {
                    Test(() -> Str("/*")); 
                    SsComment(false);
                },
                () -> {
                    TestNot(() -> Str("/*"), () -> "NOT begin slash-star comment");
                    TestNot(() -> Str("*/"), () -> "NOT end slash-star comment");
                    AnyChar();
                }
            );
        });
    }

    private void SingleQuotedStringContent() {
        runRule("SingleQuotedStringContent", () -> {
            ZeroOrMore(() -> {
                FirstOf(
                    () -> EscapedStringContent(), 
                    () -> StringContentTemplate(), 
                    () -> LiteralStringContent(false)
                );
            });
        });
    }

    private void EscapedStringContent() {
        runRule("EscapedStringContent", () -> {
            markRuleStart();
            OneOrMore(() -> {
                MatchChar('\\');
                NoneOf("\r\n");
            });
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange()));
        });
    }

    private void LiteralStringContent(boolean tripleQuoted) {
        runRule("LiteralStringContent", () -> {
            markRuleStart();
            OneOrMore(() -> {
                TestNot(() -> {
                    FirstOf(
                        () -> { 
                            if (tripleQuoted) {
                                Str("\"\"\"");
                            }
                            else {
                                MatchChar('"'); 
                            }
                        },
                        () -> Str("${"), 
                        () -> MatchChar('\\')
                    );
                }, () -> "NOT " + (tripleQuoted ? escapeString("\"\"\"") : escapeChar('"')) + ", " +
                    escapeString("${") + ", " + escapeChar('\\'));
                if (tripleQuoted) {
                    AnyChar();
                }
                else {
                    NoneOf("\r\n");
                }
            });
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange()));
        });        
    }

    void TripleQuotedStringContent() {
        runRule("TripleQuotedStringContent", () -> {
            ZeroOrMore(() -> {
                FirstOf(
                    () -> StringContentTemplate(), 
                    () -> LiteralStringContent(true)
                );
            });
        });
    }

    void StringContentTemplate() {
        runRule("StringContentTemplate", () -> {
            markRuleStart();
            Str("${");
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_STRING_TEMPLATE_START, matchRange()));
            
            ZeroOrMore(() -> TopLevelObject());
            
            markRuleStart();
            MatchChar('}');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_STRING_TEMPLATE_END, matchRange()));
        });
    }

    void ImportContent(List<IndexRange> importStatement) {
        runRule("ImportContent", () -> {            
            markRuleStart();
            IdOrNum();
            markRuleEnd();
            importStatement.add(matchRange());

            ZeroOrMore(() -> {
                Separators();

                markRuleStart();
                MatchChar('.'); 
                markRuleEnd();
                IndexRange temp = matchRange();
                
                Separators();
                
                markRuleStart();
                IdOrNum();
                markRuleEnd();
                importStatement.add(temp);
                importStatement.add(matchRange());
            });
            Opt(() -> {
                Separators(); 

                markRuleStart();
                MatchChar('.'); 
                markRuleEnd();
                IndexRange temp = matchRange();
                
                Separators();
                
                markRuleStart();
                MatchChar('*');
                markRuleEnd();
                importStatement.add(temp);
                importStatement.add(matchRange());
            });
        });
    }

    void PackageContent() {
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

    void Separators() {
        runRule("Separators", () -> {
            ZeroOrMore(() -> {
                FirstOf(
                    () -> NonNewlineWhitespace(false), 
                    () -> Newline(false), 
                    () -> DsComment(false), 
                    () -> SsComment(false)
                );
            });
        });        
    }

    void IdOrNum() {
        runRule("IdOrNum", () -> {
            TestNot(() -> Keywords(true), () -> "NOT any of select keywords");
            KotlinIdOrNumContent();
        });
    }

    void KotlinIdOrNumContent() {
        runRule("KotlinIdOrNumContent", () -> {
            FirstOf(
                () -> OneOrMore(() -> SimpleIdOrNumContent()),
                () -> BackTickString()
            );
        });
        
    }

    void BackTickString() {
        runRule("BackTickString", () -> {
            MatchChar('`');
            BackTickStringContent();
            MatchChar('`');
        });
    }

    void BackTickStringContent() {
        runRule("BackTickStringContent", () -> {
            ZeroOrMore(() -> {
                TestNot(() -> MatchChar('`'), () -> "NOT " + escapeChar('`'));
                NoneOf("\r\n");
            });
        });
    }

    void SimpleIdOrNumContent() {
        runRule("SimpleIdOrNumContent", () -> {
            // Kotlin's definition of valid identifiers is a subset of Java's own,
            // and maps to Character.isLetter(), LETTER_NUMBER, underscore and Character.isDigit()
            Char(chObj -> {
                char ch = chObj.charValue();
                if (ch == '_' || Character.isLetter(ch) || 
                        Character.getType(ch) == Character.LETTER_NUMBER) {
                    return true;
                }
                else if (Character.isDigit(ch)) {
                    return true;
                }
                return false;
            }, 
            "valid Kotlin identifier character");
        });
    }

    void Keywords(boolean validate) {
        runRule("Keywords", () -> {
            // keep updated with all usages of Keyword() below.
            FirstOf(
                () -> Str("import"),
                () -> Str("package"),
                validate ? () -> Str("as") : null
            );
            TestNot(() -> SimpleIdOrNumContent(), () -> "NOT SimpleIdOrNumContent");
        });
    }

    void Keyword(String s) {
        runRule("Keyword", () -> {            
            Str(s);
            TestNot(() -> SimpleIdOrNumContent(), () -> "NOT SimpleIdOrNumContent");
        });
    }
}