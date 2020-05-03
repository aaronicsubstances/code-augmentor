package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

public interface RegexNodeVisitor {

    Object visit(LiteralStringRegexNode node);
    Object visit(ConcatRegexNode node);
    Object visit(UnionRegexNode node);
    Object visit(KleeneClosureRegexNode node);
}