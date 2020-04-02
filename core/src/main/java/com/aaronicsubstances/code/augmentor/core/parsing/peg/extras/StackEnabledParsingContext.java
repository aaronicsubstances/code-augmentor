package com.aaronicsubstances.code.augmentor.core.parsing.peg.extras;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.aaronicsubstances.code.augmentor.core.parsing.peg.ParsingContext;

public class StackEnabledParsingContext extends ParsingContext<StackEnabledParsingState> {
    private final Map<Class<?>, Stack<Object>> valueStackMap = new HashMap<>();

    public StackEnabledParsingContext(String content) {
        super(content);
    }

    public Map<Class<?>, Stack<Object>> getValueStackMap() {
        return valueStackMap;
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
            snapshot = state().clone();
            Map<Class<?>, Integer> valueStackSizes = new HashMap<>();
            for (Map.Entry<Class<?>, Stack<Object>> e : valueStackMap.entrySet()) {
                valueStackSizes.put(e.getKey(), e.getValue().size());
            }
            snapshot.setValueStackSizes(valueStackSizes);
        }

        @Override
        public void restore() {
            checkSnapshot();
            setState(snapshot);
            snapshot = null;
            Map<Class<?>, Integer> valueStackSizes = state().getValueStackSizes();
            for (Map.Entry<Class<?>, Stack<Object>> e : valueStackMap.entrySet()) {
                Stack<Object> stack = e.getValue();
                if (valueStackSizes.containsKey(e.getKey())) {
                    int sizeToKeep = valueStackSizes.get(e.getKey());
                    while (stack.size() > sizeToKeep) {
                        stack.pop();
                    }
                }
                else {
                    stack.clear();
                }
            }
        }

        @Override
        public void restoreClone() {
            checkSnapshot();
            setState(snapshot.clone());
        }

        private void checkSnapshot() {
            if (snapshot == null)
                throw new RuntimeException("cannot restore after the first call to restore()");
        }
    }
}