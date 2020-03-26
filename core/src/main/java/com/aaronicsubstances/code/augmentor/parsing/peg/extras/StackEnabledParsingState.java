package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.Map;

import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingState;

public class StackEnabledParsingState extends ParsingState<StackEnabledParsingState> {
    private Map<Class<?>, Integer> valueStackSizes;

    public Map<Class<?>, Integer> getValueStackSizes() {
        return valueStackSizes;
    }

    public void setValueStackSizes(Map<Class<?>, Integer> valueStackSizes) {
        this.valueStackSizes = valueStackSizes;
    }
}