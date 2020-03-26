package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

public class LogData {
    public final int depth;
    public final boolean supressLogs;

    public LogData(int depth, boolean supressLogs) {
        this.depth = depth;
        this.supressLogs = supressLogs;
    }
}