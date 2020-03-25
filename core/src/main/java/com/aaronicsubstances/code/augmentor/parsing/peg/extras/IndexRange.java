package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

public class IndexRange {
    public final int start;
    public final int end;

    public IndexRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "IndexRange{start=" + start + ", end=" + end + "}";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + end;
        result = prime * result + start;
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
        IndexRange other = (IndexRange) obj;
        if (end != other.end)
            return false;
        if (start != other.start)
            return false;
        return true;
    }
}