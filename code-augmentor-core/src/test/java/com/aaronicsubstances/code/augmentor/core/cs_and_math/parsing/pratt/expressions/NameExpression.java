package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions;

/**
 * A simple variable name expression like "abc".
 */
public class NameExpression implements Expression {
    private final String mName;
    
    public NameExpression(String name) {
        mName = name;
    }
  
    public String getName() { return mName; }
  
    @Override
    public void print(StringBuilder builder) {
        builder.append(mName);
    }
}
