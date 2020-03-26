package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingState;

public class StackEnabledParsingState extends ParsingState<StackEnabledParsingState> {
    private int sizeToKeep;

    public int getSizeToKeep() {
        return sizeToKeep;
    }

    public void setSizeToKeep(int sizeToKeep) {
        this.sizeToKeep = sizeToKeep;
    }
}