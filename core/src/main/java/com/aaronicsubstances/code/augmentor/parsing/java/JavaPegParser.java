package com.aaronicsubstances.code.augmentor.parsing.java;

import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.parsing.PegToken;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.IndexRange;
import org.parboiled.support.Var;

//@BuildParseTree
public class JavaPegParser extends BaseParser<PegToken> {

    public Rule Parse() {
        return Sequence(ZeroOrMore(TopLevelObject()), EOI);
    }

    Rule TopLevelObject() {
        return FirstOf(DsComment(true), SsComment(true), NonNewlineWhitespace(true),
            Newline(true), StringExpression(), PackageStatement(), ImportStatement(),
            IdOrNumOrKeyword(), OtherToken());
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
    Rule StringExpression() {
        return Sequence('"', push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange())),
            StringContent(),  push(new PegToken(PegToken.TYPE_LITERAL_STRING_CONTENT, matchRange())),
            '"', push(new PegToken(PegToken.TYPE_STRING_DELIMITER, matchRange())));
    }

    @SuppressSubnodes
    Rule ImportStatement() {
        Var<List<IndexRange>> importStatement = new Var<>(new ArrayList<>());
        return Sequence(
                Sequence(Keyword("import"), addToList(importStatement, matchRange()),
                Optional(Separators(), 
                    Keyword("static"), addToList(importStatement, matchRange())),
                Separators(), 
                ImportContent(importStatement), Separators(), ';'),
            push(new PegToken(PegToken.TYPE_IMPORT, matchRange(), importStatement.get())));
    }

    boolean addToList(Var<List<IndexRange>> importStatement, IndexRange item) {
        importStatement.get().add(item);
        return true;
    }

    @SuppressSubnodes
    Rule PackageStatement() {
        return Sequence(Sequence(Keyword("package"), Separators(), PackageContent(), Separators(), ';'),
            push(new PegToken(PegToken.TYPE_PACKAGE, matchRange())));
    }

    @SuppressSubnodes
    Rule IdOrNumOrKeyword() {
        return Sequence(Sequence(TestNot(Keywords(false)), OneOrMore(IdOrNumContent())),
            push(new PegToken(PegToken.TYPE_QUASI_ID, matchRange())));
    }

    @SuppressSubnodes
    Rule OtherToken() {
        return Sequence(TestNot(Keywords(false)), TestNot(FirstOf("/*", '"')), ANY,
            push(new PegToken(PegToken.TYPE_OTHER, matchRange())));
    }

    Rule SsCommentContent() {
        return Sequence(TestNot("*/"), ANY);
    }

    Rule StringContent() {
        return ZeroOrMore(Sequence(TestNot('"'),
            FirstOf(EscapedStringContent(), LiteralStringContent())));
    }

    Rule EscapedStringContent() {
        return Sequence("\\", NoneOf("\r\n"));
    }

    Rule LiteralStringContent() {
        return NoneOf("\r\n");
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
        return Sequence(TestNot(Keywords(true)), OneOrMore(IdOrNumContent()));
    }

    Rule IdOrNumContent() {
        return FirstOf(CharRange('A', 'Z'), CharRange('a', 'z'), '_', CharRange('0', '9'));
    }

    Rule Keywords(boolean validate) {
        // keep updated with all usages of Keyword() below.
        return Sequence(FirstOf("import", "package", validate ? "static" : NOTHING), 
            TestNot(IdOrNumContent()));
    }

    Rule Keyword(String s) {
        return Sequence(s, TestNot(IdOrNumContent()));
    }
}