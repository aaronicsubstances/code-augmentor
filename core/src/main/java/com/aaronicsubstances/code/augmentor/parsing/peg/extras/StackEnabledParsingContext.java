package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.Stack;

import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext;

public class StackEnabledParsingContext<T> extends ParsingContext<StackEnabledParsingState<T>> {
    private final Stack<T> valueStack = new Stack<>();

    public StackEnabledParsingContext(String content) {
        super(content);
    }

    public Stack<T> getValueStack() {
        return valueStack;
    }

    @Override
    protected StackEnabledParsingState<T> createInitialState() {
        return new StackEnabledParsingState<>();
    }

    @Override
    public StateSnapshot snapshot() {
        return new StateSnapshotImpl();
    }

    /**
     * Implementation of {@link StateSnapshot}
     */
    private class StateSnapshotImpl implements StateSnapshot {
        StackEnabledParsingState<T> snapshot;

        public StateSnapshotImpl() {
            snapshot = state.clone();
            snapshot.setSizeToKeep(valueStack.size());
        }

        @Override
        public void restore() {
            checkSnapshot();
            state = snapshot;
            snapshot = null;
            while (valueStack.size() > state.getSizeToKeep()) {
                valueStack.pop();
            }
        }

        @Override
        public void restoreClone() {
            checkSnapshot();
            state = snapshot.clone();
        }

        private void checkSnapshot() {
            if (snapshot == null)
                throw new RuntimeException("cannot restore after the first call to restore()");
        }
    }
}