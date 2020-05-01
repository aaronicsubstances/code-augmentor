package com.aaronicsubstances.code.augmentor.core.fsa;

public class RegexNode {
    public static final int TYPE_SYMBOL = 1;
    public static final int TYPE_KLEENE_CLOSURE = 2;
    public static final int TYPE_CONCATENATION = 3;
    public static final int TYPE_UNION = 4;

    private final int type;
    private final RegexNode leftChild;
    private final RegexNode rightChild;
    private final String symbol;

    public RegexNode(String symbol) {
        this(TYPE_SYMBOL, null, null, symbol);
    }

    public RegexNode(RegexNode leftChild) {
        this(TYPE_KLEENE_CLOSURE, leftChild, null, null);
    }

    public RegexNode(int type, RegexNode leftChild, RegexNode rightChild) {
        this(type, leftChild, rightChild, null);
    }

    public RegexNode(int type, RegexNode leftChild, RegexNode rightChild, String symbol) {
        this.type = type;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.symbol = null;
    }

    public int getType() {
        return type;
    }

    public RegexNode getLeftChild() {
        return leftChild;
    }

    public RegexNode getRightChild() {
        return rightChild;
    }

    public String getSymbol() {
        return symbol;
    }
}