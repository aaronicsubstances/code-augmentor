package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.parsing.peg.NoMatchException;
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

            Opt(() -> Shebang());
            ZeroOrMore(() -> TopLevelObject());
            EOI();

            endLogMajorSuccess(ruleName);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void Shebang() {
        final String ruleName = "Shebang";
        try {
            beginLog(ruleName);

            markRuleStart();
            Str("#!");
            ZeroOrMore(() -> NoneOf("\r\n"));
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_SHEBANG, matchRange()));

            endLog(ruleName, null);
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

            endLogMajorSuccess(ruleName);
        }
        catch (NoMatchException ex) {
            endLog(ruleName, ex);
            throw ex;
        }
    }

    void BracedBlock() {
        final String ruleName = "BracedBlock";
        try {
            beginLog(ruleName);

            markRuleStart();
            MatchChar('{');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_BRACED_BLOCK_START, matchRange()));

            ZeroOrMore(() -> TopLevelObject());
            
            markRuleStart();
            MatchChar('}');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_BRACED_BLOCK_END, matchRange()));

            endLog(ruleName, null);
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
            beginLog(ruleName);
            
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
            OneOrMore(() -> { AnyOf(" \t\f"); });
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

    void SingleQuotedStringExpression() {
        final String ruleName = "SingleQuotedStringExpression";
        try {
            beginLog(ruleName);
            
            markRuleStart();
            MatchChar('"');
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange()));
            
            SingleQuotedStringContent();
            
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

    void TripleQuotedStringExpression() {
        final String ruleName = "TripleQuotedStringExpression";
        try {
            beginLog(ruleName);

            markRuleStart();
            Str("\"\"\"");
            markRuleEnd(); 
            push(new PegToken(PegToken.TYPE_TRIPLE_QUOTED_STRING_DELIMITER, matchRange()));
            
            TripleQuotedStringContent();
            
            markRuleStart();
            Str("\"\"\"");
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_TRIPLE_QUOTED_STRING_DELIMITER, matchRange()));

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
            Opt(() -> {
                Separators();
                MatchChar(';');
            });
            markRuleEnd();
            push(new PegToken(PegToken.TYPE_PACKAGE, matchRange()));

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
            TestNot(() -> Keywords(false), "NOT any of select few keywords");
            KotlinIdOrNumContent();
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
                () -> Str("/*"), 
                () -> Str("\"\"\""), 
                () -> MatchChar('"'), 
                () -> MatchChar('`'), 
                () -> MatchChar('{'), 
                () -> MatchChar('}')
            ), "NOT any of " + escapeString("/*") + ", " + escapeString("\"\"\"") + ", " +
                escapeChar('"') + ", " + escapeChar('`') + ", " + escapeChar('{') +
                escapeChar('}'));
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
        FirstOf(
            () -> {
                Test(() -> Str("/*")); 
                SsComment(false);
            },
            () -> {
                TestNot(() -> Str("/*"), "NOT begin slash-star comment");
                TestNot(() -> Str("*/"), "NOT end slash-star comment");
                AnyChar();
            }
        );
    }

    private void SingleQuotedStringContent() {
        ZeroOrMore(() -> {
            FirstOf(
                () -> EscapedStringContent(), 
                () -> StringContentTemplate(), 
                () -> LiteralStringContent(false)
            );
        });
    }

    private void EscapedStringContent() {
        markRuleStart();
        OneOrMore(() -> {
            MatchChar('\\');
            NoneOf("\r\n");
        });
        markRuleEnd();
        push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange()));
    }

    private void LiteralStringContent(boolean tripleQuoted) {
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
                    () -> { Str("${"); }, 
                    () -> { MatchChar('\\'); }
                );
            }, "NOT " + (tripleQuoted ? escapeString("\"\"\"") : escapeChar('"')) + ", " +
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
    }

    void TripleQuotedStringContent() {
        ZeroOrMore(() -> {
            FirstOf(
                () -> StringContentTemplate(), 
                () -> LiteralStringContent(true)
            );
        });
    }

    void StringContentTemplate() {
        markRuleStart();
        Str("${");
        markRuleEnd();
        push(new PegToken(PegToken.TYPE_STRING_TEMPLATE_START, matchRange()));
        
        ZeroOrMore(() -> TopLevelObject());
        
        markRuleStart();
        MatchChar('}');
        markRuleEnd();
        push(new PegToken(PegToken.TYPE_STRING_TEMPLATE_END, matchRange()));
    }

    void ImportContent(List<IndexRange> importStatement) {
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

    void PackageContent() {
        IdOrNum();
        ZeroOrMore(() -> {
            Separators();
            MatchChar('.'); 
            Separators();
            IdOrNum();
        });
    }

    void Separators() {
        ZeroOrMore(() -> {
            FirstOf(
                () -> NonNewlineWhitespace(false), 
                () -> Newline(false), 
                () -> DsComment(false), 
                () -> SsComment(false)
            );
        });
    }

    void IdOrNum() {
        TestNot(() -> Keywords(true), "NOT any of select keywords");
        KotlinIdOrNumContent();
    }

    void KotlinIdOrNumContent() {
        FirstOf(
            () -> OneOrMore(() -> SimpleIdOrNumContent()),
            () -> BackTickString()
        );
    }

    void BackTickString() {
        MatchChar('`');
        BackTickStringContent();
        MatchChar('`');
    }

    void BackTickStringContent() {
        ZeroOrMore(() -> {
            TestNot(() -> MatchChar('`'), "NOT " + escapeChar('`'));
            NoneOf("\r\n");
        });
    }

    void SimpleIdOrNumContent() {
        // Kotlin's definition of valid identifiers is a subset of Java's own,
        // and maps to Character.isLetter(), LETTER_NUMBER, underscore and Character.isDigit()
        Char(ch -> {
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
    }

    void Keywords(boolean validate) {
        // keep updated with all usages of Keyword() below.
        FirstOf(
            () -> { 
                Str("import"); },
            () -> { 
                Str("package"); },
            validate ? () -> {
                Str("as");
            } : null
        );
        TestNot(() -> SimpleIdOrNumContent(), "NOT SimpleIdOrNumContent");
    }

    void Keyword(String s) {
        Str(s);
        TestNot(() -> SimpleIdOrNumContent(), "NOT SimpleIdOrNumContent");
    }
}