package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConcatRegexNode implements RegexNode {
    public static final String OP_REPR_KEY = "Concatenation";

    private final List<RegexNode> children;

    public ConcatRegexNode(RegexNode... children) {
        this(Arrays.asList(children));
    }

    public ConcatRegexNode(List<RegexNode> children) {
        this.children = children;
    }

    @Override
    public Object accept(RegexNodeVisitor visitor) {
        return visitor.visit(this);
    }

    public List<RegexNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    @Override
    public String toString(Map<String, String> opTokens) {
        String opToken = opTokens == null ? null : opTokens.get(OP_REPR_KEY);
        if (opToken == null) {
            opToken = " - ";
        }
        StringBuilder repr = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            RegexNode child = children.get(i);
            if (i > 0) {
                repr.append(opToken);
            }
            repr.append(child.toString(opTokens));
        }
        return repr.toString();
    }
}