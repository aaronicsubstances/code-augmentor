package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FsaEquivalenceMatcher extends TypeSafeMatcher<FiniteStateAutomaton> {
    private final FiniteStateAutomaton comparisonTarget;

    public FsaEquivalenceMatcher(FiniteStateAutomaton comparisonTarget) {
        this.comparisonTarget = comparisonTarget;
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(comparisonTarget);
    }

    @Override
    protected boolean matchesSafely(FiniteStateAutomaton item) {
        return FiniteStateAutomaton.areEquivalent(item, comparisonTarget);
    }
}