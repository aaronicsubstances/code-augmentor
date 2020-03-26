package com.aaronicsubstances.code.augmentor.parsing.java;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.parsing.peg.NoMatchException;
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
        List<PegToken> tokenList = new ArrayList<>();
        for (Object stackItem : getParsingContext().getValueStack()) {
            if (stackItem instanceof PegToken) {
                // add in reverse.
                tokenList.add(0, (PegToken)stackItem);
            }
        }
        return tokenList;
    }

    void EntireInput() {
        final String ruleName = "EntireInput";
        try {
            beginLog(ruleName);

            ZeroOrMore(() -> {
                TopLevelObject();
            });
            EOI();

            endLogMajorSuccess(ruleName);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void TopLevelObject() {
        final String ruleName = "TopLevelObject";
        try {
            beginLog(ruleName);

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

            endLogMajorSuccess(ruleName);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void DsComment(boolean significant) {
        final String ruleName = "DsComment";
        try {
            beginLog(ruleName, significant);

            markRuleStart();
            Str("//");
            ZeroOrMore(() -> {
                NoneOf("\r\n"); });
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_DS_COMMENT, matchRange()));
            }

            endLog(ruleName, significant, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, significant, ex);
            throw ex;
        }
    }

    void SsComment(boolean significant) {
        final String ruleName = "SsComment";
        try {
            beginLog(ruleName, significant);

            markRuleStart();
            Str("/*");
            ZeroOrMore(() -> SsCommentContent());
            Str("*/");
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_SS_COMMENT, matchRange()));
            }

            endLog(ruleName, significant, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, significant, ex);
            throw ex;
        }
    }

    void NonNewlineWhitespace(boolean significant) {
        final String ruleName = "NonNewlineWhitespace";
        try {
            beginLog(ruleName, significant);

            markRuleStart();
            OneOrMore(() -> {
                AnyOf(" \t\f");
            });
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_NON_NEWLINE_WS, matchRange()));
            }

            endLog(ruleName, significant, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, significant, ex);
            throw ex;
        }
    }

    void Newline(boolean significant) {
        final String ruleName = "Newline";
        try {
            beginLog(ruleName, significant);

            markRuleStart();
            FirstOf(
                () -> { Str("\r\n"); }, 
                () -> { MatchChar('\r'); }, 
                () -> { MatchChar('\n'); }
            );
            markRuleEnd();
            if (significant) {
                push(new PegToken(PegToken.TYPE_NEWLINE, matchRange()));
            }

            endLog(ruleName, significant, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, significant, ex);
            throw ex;
        }
    }

    void StringExpression() {
        final String ruleName = "StringExpression";
        try {
            beginLog(ruleName);

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

            endLog(ruleName, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void ImportStatement() {
        final String ruleName = "ImportStatement";
        try {
            beginLog(ruleName);

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

            endLog(ruleName, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void PackageStatement() {
        final String ruleName = "PackageStatement";
        try {
            beginLog(ruleName);

            markRuleStart();
            Keyword("package"); 
            Separators(); 
            PackageContent();
            Separators();
            MatchChar(';');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_PACKAGE, matchRange()));
            
            endLog(ruleName, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void IdOrNumOrKeyword() {
        final String ruleName = "IdOrNumOrKeyword";
        try {
            beginLog(ruleName);

            markRuleStart();
            TestNot(() -> Keywords(false), "NOT select few keywords");
            OneOrMore(() -> IdOrNumContent());
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_QUASI_ID, matchRange()));

            endLog(ruleName, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void OtherToken() {
        final String ruleName = "OtherToken";
        try {
            beginLog(ruleName);

            markRuleStart();
            TestNot(() -> Keywords(false), "NOT any of select few keywords");
            TestNot(() -> FirstOf(
                () -> { Str("/*"); }, 
                () -> { MatchChar('"'); } 
            ), "NOT any of " + escapeString("/*") + ", " + escapeChar('"'));
            AnyChar();
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_OTHER, matchRange()));

            endLog(ruleName, null);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    private void SsCommentContent() {
        TestNot(() -> { Str("*/"); }, "NOT " + escapeString("*/"));
        AnyChar();
    }

    private void StringContent() {
        ZeroOrMore(() -> {
            TestNot(() -> { MatchChar('"'); }, "NOT " + escapeChar('"'));
            FirstOf(() -> EscapedStringContent(), () -> LiteralStringContent());
        });
    }

    private void EscapedStringContent() {
        MatchChar('\\');
        NoneOf("\r\n");
    }

    private void LiteralStringContent() {
        NoneOf("\r\n");
    }

    private void ImportContent(List<IndexRange> importStatement) {
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
    }

    private void PackageContent() {
        IdOrNum();
        ZeroOrMore(() -> {
            Separators();
            MatchChar('.');
            Separators();
            IdOrNum();
        });
    }

    private void Separators() {
        ZeroOrMore(() ->
            FirstOf(
                () -> NonNewlineWhitespace(false),
                () -> Newline(false), 
                () -> DsComment(false), 
                () -> SsComment(false)
            )
        );
    }

    private void IdOrNum() {
        TestNot(() -> Keywords(true), "NOT any of select Keywords");
        OneOrMore(() -> IdOrNumContent());
    }

    private void IdOrNumContent() {
        Char(Character::isJavaIdentifierPart, "valid Java identifier character");
    }

    private void Keywords(boolean validate) {
        // keep updated with all usages of Keyword() below.
        FirstOf(
            () -> { 
                Str("import"); },
            () -> { 
                Str("package"); },
            validate ? () -> {
                Str("static");
            } : null
        );
        TestNot(() -> IdOrNumContent(), "NOT IdOrNumContent");
    }

    private void Keyword(String s) {
        Str(s);
        TestNot(() -> IdOrNumContent(), "NOT IdOrNumContent");
    }
}