package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.Map;

public class LiteralStringRegexNode implements RegexNode {
    public static final String EMPTY_STRING_REPR_KEY = "EmptyString";
    public static final String CONCAT_OP_KEY = "StringConcatenation";

    private final int[] literalString;

    public LiteralStringRegexNode(int symbol) {
        this(new int[]{ symbol });
    }

    public LiteralStringRegexNode(int[] literalString) {
        this.literalString = literalString;
    }

    @Override
    public Object accept(RegexNodeVisitor visitor) {
        return visitor.visit(this);
    }

    public int[] getLiteralString() {
        return literalString;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    @Override
    public String toString(Map<String, String> opTokens) {
        String opToken = opTokens == null ? null : opTokens.get(CONCAT_OP_KEY);
        if (opToken == null) {
            opToken = ":";
        }
        if (literalString.length == 0) {
            String emptyStringRepr = opTokens == null ? null : opTokens.get(EMPTY_STRING_REPR_KEY);
            if (emptyStringRepr == null) {
                emptyStringRepr = "e";
            }
            return emptyStringRepr;
        }
        StringBuilder repr = new StringBuilder();
        for (int i = 0; i < literalString.length; i++) {
            int symbol = literalString[i];
            if (i > 0) {
                repr.append(opToken);
            }
            repr.append(symbol);
        }
        return repr.toString();
    }

	@Override
	public RegexNode generateCopy() {
        int[] literalStringCopy = null;
        if (this.literalString != null) {
            literalStringCopy = new int[this.literalString.length];
            System.arraycopy(this.literalString, 0, literalStringCopy, 0, 
                literalStringCopy.length);
        }
        LiteralStringRegexNode copy = new LiteralStringRegexNode(literalStringCopy);
        return copy;
	}
}