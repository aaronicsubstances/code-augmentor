package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing;

public class GenericToken {
    public int type;
    public String text;

    public GenericToken() {
    }

    public GenericToken(int type, String text) {
        this.type = type;
        this.text = text;
    }
}