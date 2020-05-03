package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.Map;

public class KleeneClosureRegexNode implements RegexNode {
    public static final String OP_REPR_KEY = "KleeneClosure";

    private final RegexNode child;

    public KleeneClosureRegexNode(RegexNode child) {
        this.child = child;
    }

    @Override
    public Object accept(RegexNodeVisitor visitor) {
        return visitor.visit(this);
    }

    public RegexNode getChild() {
        return child;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    @Override
    public String toString(Map<String, String> opTokens) {
        String opToken = opTokens == null ? null : opTokens.get(OP_REPR_KEY);
        if (opToken == null) {
            opToken = "*";
        }
        StringBuilder repr = new StringBuilder();
        repr.append("(").append(child).append(")").append(opToken);
        return repr.toString();
    }
}