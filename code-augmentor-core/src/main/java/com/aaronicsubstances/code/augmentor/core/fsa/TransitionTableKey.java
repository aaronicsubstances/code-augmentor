package com.aaronicsubstances.code.augmentor.core.fsa;

class TransitionTableKey {
    public final int state;
    public final String symbol;

    public TransitionTableKey(int state, String symbol) {
        this.state = state;
        this.symbol = symbol;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + state;
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TransitionTableKey other = (TransitionTableKey) obj;
        if (state != other.state)
            return false;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TransitionTableKey [state=" + state + ", symbol=" + symbol + "]";
    }
}