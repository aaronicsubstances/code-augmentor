package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions;

import java.util.List;

/**
 * A function call like "a(b, c, d)".
 */
public class CallExpression implements Expression {
    private final Expression mFunction;
    private final List<Expression> mArgs;

    public CallExpression(Expression function, List<Expression> args) {
        mFunction = function;
        mArgs = args;
    }
  
    @Override
    public void print(StringBuilder builder) {
        mFunction.print(builder);
        builder.append("(");
        for (int i = 0; i < mArgs.size(); i++) {
            mArgs.get(i).print(builder);
            if (i < mArgs.size() - 1) builder.append(", ");
        }
        builder.append(")");
    }
}
