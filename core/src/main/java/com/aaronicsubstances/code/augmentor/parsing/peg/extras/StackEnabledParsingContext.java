package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.LinkedList;

import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext;

public class StackEnabledParsingContext extends ParsingContext<StackEnabledParsingState> {
    private final LinkedList<Object> valueStack = new LinkedList<>();

    public StackEnabledParsingContext(String content) {
        super(content);
    }

    public LinkedList<Object> getValueStack() {
        return valueStack;
    }

    @Override
    protected StackEnabledParsingState createInitialState() {
        return new StackEnabledParsingState();
    }

    @Override
    public StateSnapshot snapshot() {
        return new StateSnapshotImpl();
    }

    /**
     * Implementation of {@link StateSnapshot}
     */
    private class StateSnapshotImpl implements StateSnapshot {
        StackEnabledParsingState snapshot;

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