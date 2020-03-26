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

    void StringExpression() {
        runRule("StringExpression", true, () -> {
            markRuleStart();
            MatchChar('"');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange()));
            
            markRuleStart();
            StringContent();
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange()));

            markRuleStart();
            MatchChar('"');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange()));
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

            Opt(() -> {
                Separators();
                markRuleStart();
                Keyword("static");
                markRuleEnd();
                importStatement.add(matchRange());
            });
            Separators();
            ImportContent(importStatement);
            Separators();
            MatchChar(';');

            markRuleEnd();
            push(new PegToken(PegToken.TYPE_IMPORT, matchRange(), importStatement));
        });
    }

    void PackageStatement() {
        runRule("PackageStatement", true, () -> {
            markRuleStart();
            Keyword("package"); 
            Separators(); 
            PackageContent();
            Separators();
            MatchChar(';');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_PACKAGE, matchRange()));
        });
    }

    void IdOrNumOrKeyword() {
        runRule("IdOrNumOrKeyword", true, () -> {
            markRuleStart();
            TestNot(() -> Keywords(false), () -> "NOT select few keywords");
            OneOrMore(() -> IdOrNumContent());
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
                () -> MatchChar('"') 
            ), () -> "NOT any of " + escapeString("/*") + ", " + escapeChar('"'));
            AnyChar();
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_OTHER, matchRange()));
        });
    }

    private void SsCommentContent() {
        runRule("SsCommentContent", () -> {
            TestNot(() -> Str("*/"), () -> "NOT " + escapeString("*/"));
            AnyChar();
        });
    }

    private void StringContent() {
        runRule("StringContent", () -> {
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