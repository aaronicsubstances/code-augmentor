package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UnionRegexNode implements RegexNode {
    public static final String OP_REPR_KEY = "Union";

    private final List<RegexNode> children;

    public UnionRegexNode(RegexNode... children) {
        this(Arrays.asList(children));
    }

    public UnionRegexNode(List<RegexNode> children) {
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
            opToken = " | ";
        }
        StringBuilder repr = new StringBuilder();
        repr.append("(");
        for (int i = 0; i < children.size(); i++) {
            RegexNode child = children.get(i);
            if (i > 0) {
                repr.append(opToken);
            }
            repr.append(child.toString(opTokens));
        }
        repr.append(")");
        return repr.toString();
    }

	@Override
	public RegexNode generateCopy() {
        List<RegexNode> copyChildren = null;
        if (this.children != null) {
            copyChildren = new ArrayList<>();
            for (RegexNode child : this.children) {
                if (child == null) {
                    copyChildren.add(null);
                }
                else {
                    copyChildren.add(child.generateCopy());
                }
            }
        }
        UnionRegexNode copy = new UnionRegexNode(copyChildren);
        return copy;
	}
}