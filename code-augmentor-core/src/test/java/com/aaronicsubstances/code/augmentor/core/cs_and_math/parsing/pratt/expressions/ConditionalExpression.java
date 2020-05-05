package com.aaronicsubstances.code.augmentor.core.cs_and_math.parsing.pratt.expressions;

/**
 * A ternary conditional expression like "a ? b : c".
 */
public class ConditionalExpression implements Expression {
    private final Expression mCondition;
    private final Expression mThenArm;
    private final Expression mElseArm;

    public ConditionalExpression(
            Expression condition, Expression thenArm, Expression elseArm) {
        mCondition = condition;
        mThenArm   = thenArm;
        mElseArm   = elseArm;
    }
  
    @Override
    public void print(StringBuilder builder) {
        builder.append("(");
        mCondition.print(builder);
        builder.append(" ? ");
        mThenArm.print(builder);
        builder.append(" : ");
        mElseArm.print(builder);
        builder.append(")");
    }
}
