package com.aaronicsubstances.code.augmentor.parsing.kotlin;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.parsing.PegToken;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.IndexRange;
import org.parboiled.support.Var;

//@BuildParseTree
public class KotlinPegParser extends BaseParser<PegToken> {

    public Rule Parse() {
        return Sequence(Optional(Shebang()), ZeroOrMore(TopLevelObject()), EOI);
    }

    @SuppressSubnodes
    Rule Shebang() {
        return Sequence(Sequence("#!", ZeroOrMore(NoneOf("\r\n"))),
            push(new PegToken(PegToken.TYPE_SHEBANG, matchRange())));
    }

    Rule TopLevelObject() {
        return FirstOf(BracedBlock(), 
            DsComment(true), SsComment(true), 
            NonNewlineWhitespace(true),  Newline(true), 
            TripleQuotedStringExpression(), SingleQuotedStringExpression(), 
            PackageStatement(), ImportStatement(), IdOrNumOrKeyword(), 
            OtherToken());
    }

    Rule BracedBlock() {
        return Sequence('{', push(new PegToken(PegToken.TYPE_BRACED_BLOCK_START, matchRange())),
            ZeroOrMore(TopLevelObject()),
            '}', push(new PegToken(PegToken.TYPE_BRACED_BLOCK_END, matchRange())));
    }

    @SuppressSubnodes
    Rule DsComment(boolean significant) {
        return Sequence(Sequence("//", ZeroOrMore(NoneOf("\r\n"))),
            !significant || push(new PegToken(PegToken.TYPE_DS_COMMENT, matchRange())));
    }

    @SuppressSubnodes
    Rule SsComment(boolean significant) {
        return Sequence(Sequence("/*", ZeroOrMore(SsCommentContent()), "*/"),
            !significant || push(new PegToken(PegToken.TYPE_SS_COMMENT, matchRange())));
    }

    @SuppressSubnodes
    Rule NonNewlineWhitespace(boolean significant) {
        return Sequence(OneOrMore(AnyOf(" \t\f")),
            !significant || push(new PegToken(PegToken.TYPE_NON_NEWLINE_WS, matchRange())));
    }

    @SuppressSubnodes
    Rule Newline(boolean significant) {
        return Sequence(FirstOf("\r\n", '\r', '\n'),
            !significant || push(new PegToken(PegToken.TYPE_NEWLINE, matchRange())));
    }

    @SuppressSubnodes
    Rule SingleQuotedStringExpression() {
        return Sequence('"', push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange())),
            SingleQuotedStringContent(),
            '"', push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange())));
    }

    @SuppressSubnodes
    Rule TripleQuotedStringExpression() {
        return Sequence("\"\"\"", push(new PegToken(PegToken.TYPE_TRIPLE_QUOTED_STRING_DELIMITER, matchRange())),
            TripleQuotedStringContent(),
            "\"\"\"", push(new PegToken(PegToken.TYPE_TRIPLE_QUOTED_STRING_DELIMITER, matchRange())));
    }

    @SuppressSubnodes
    Rule PackageStatement() {
        return Sequence(Sequence(Keyword("package"), Separators(), PackageContent(),
                Optional(Separators(), ';')),
            push(new PegToken(PegToken.TYPE_PACKAGE, matchRange())));
    }

    @SuppressSubnodes
    Rule ImportStatement() {
        Var<List<IndexRange>> importStatement = new Var<>(new ArrayList<>());
        Var<IndexRange> temp = new Var<>();
        return Sequence(
                Sequence(Keyword("import"), addToList(importStatement, matchRange()),
                Separators(), 
                ImportContent(importStatement),
                Optional( 
                    Separators(),
                    Keyword("as"), temp.set(matchRange()),
                    Separators(),
                    IdOrNum(), 
                    addToList(importStatement, temp.get()),
                    addToList(importStatement, matchRange())),                
                Optional(Separators(), ';')),
            push(new PegToken(PegToken.TYPE_IMPORT, matchRange(), importStatement.get())));
    }

    boolean addToList(Var<List<IndexRange>> importStatement, IndexRange item) {
        importStatement.get().add(item);
        return true;
    }

    @SuppressSubnodes
    Rule IdOrNumOrKeyword() {
        return Sequence(Sequence(TestNot(Keywords(false)), KotlinIdOrNumContent()),
            push(new PegToken(PegToken.TYPE_QUASI_ID, matchRange())));
    }

    @SuppressSubnodes
    Rule OtherToken() {
        return Sequence(TestNot(Keywords(false)), 
            TestNot(FirstOf("/*", "\"\"\"", '"', '`', '{', '}')), ANY,
            push(new PegToken(PegToken.TYPE_OTHER, matchRange())));
    }

    Rule SsCommentContent() {
        return FirstOf(Sequence(Test("/*"), SsComment(false)), 
            Sequence(TestNot("/*"), TestNot("*/"), ANY));
    }

    Rule SingleQuotedStringContent() {
        return ZeroOrMore(
            FirstOf(EscapedStringContent(), 
                StringContentTemplate(),
                LiteralStringContent(false)));
    }

    Rule EscapedStringContent() {
        return Sequence(OneOrMore(Sequence("\\", NoneOf("\r\n"))),
            push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange())));
    }

    Rule LiteralStringContent(boolean tripleQuoted) {
        return Sequence(OneOrMore(Sequence(TestNot(FirstOf(tripleQuoted ? "\"\"\"" : '"', "${", "\\")), 
                tripleQuoted ? ANY : NoneOf("\r\n"))),
            push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange()))
        );
    }

    Rule TripleQuotedStringContent() {
        return ZeroOrMore(
            FirstOf(StringContentTemplate(), LiteralStringContent(true)));
    }

    Rule StringContentTemplate() {
        return Sequence("${", push(new PegToken(PegToken.TYPE_STRING_TEMPLATE_START, matchRange())),
            ZeroOrMore(TopLevelObject()),
            "}", push(new PegToken(PegToken.TYPE_STRING_TEMPLATE_END, matchRange())));
    }

    Rule ImportContent(Var<List<IndexRange>> importStatement) {
        Var<IndexRange> temp = new Var<>();
        return Sequence(
            IdOrNum(), addToList(importStatement, matchRange()),
            ZeroOrMore(Sequence(
                Separators(), 
                '.', temp.set(matchRange()),
                Separators(), 
                IdOrNum(), 
                addToList(importStatement, temp.get()),
                addToList(importStatement, matchRange()))),
            Optional(
                Separators(), 
                '.', temp.set(matchRange()),
                Separators(), 
                '*', 
                addToList(importStatement, temp.get()),
                addToList(importStatement, matchRange())));
    }

    Rule PackageContent() {
        return Sequence(IdOrNum(), ZeroOrMore(Sequence(Separators(), '.', Separators(), IdOrNum())));
    }

    Rule Separators() {
        return ZeroOrMore(FirstOf(NonNewlineWhitespace(false), Newline(false), 
            DsComment(false), SsComment(false)));
    }

    Rule IdOrNum() {
        return Sequence(TestNot(Keywords(true)), KotlinIdOrNumContent());
    }

    Rule KotlinIdOrNumContent() {
        return FirstOf(OneOrMore(SimpleIdOrNumContent()),
            BackTickString());
    }

    Rule BackTickString() {
        return Sequence('`', BackTickStringContent(), '`');
    }

    Rule BackTickStringContent() {
        return ZeroOrMore(Sequence(TestNot('`'), NoneOf("\r\n")));
    }

    Rule SimpleIdOrNumContent() {
        return FirstOf(CharRange('A', 'Z'), CharRange('a', 'z'), '_', CharRange('0', '9'));
    }

    Rule Keywords(boolean validate) {
        // keep updated with all usages of Keyword() below.
        return Sequence(FirstOf("import", "package", validate ? "as" : NOTHING), 
            TestNot(SimpleIdOrNumContent()));
    }

    Rule Keyword(String s) {
        return Sequence(s, TestNot(SimpleIdOrNumContent()));
    }
}