package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.Stack;

import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingState;

public class StackEnabledParsingState<T> extends ParsingState<StackEnabledParsingState<T>> {
    private int sizeToKeep;

    public int getSizeToKeep() {
        return sizeToKeep;
    }

    public void setSizeToKeep(int sizeToKeep) {
        this.sizeToKeep = sizeToKeep;
    }
}