package com.aaronicsubstances.code.augmentor.parsing.peg;

import org.testng.annotations.*;

import com.aaronicsubstances.code.augmentor.parsing.peg.ParsingContext.StateSnapshot;

public class ParsingContextTest {

    @Test
    public void snapshotRestore() {
        DefaultParsingContext ctx = new DefaultParsingContext("foo");
        StateSnapshot sn = ctx.snapshot();
        sn.restore();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void snapshotRestoreTwice() {
        DefaultParsingContext ctx = new DefaultParsingContext("foo");
        StateSnapshot sn = ctx.snapshot();
        sn.restore();
        sn.restore();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void snapshotRestoreTwiceSecondTimeClone() {
        DefaultParsingContext ctx = new DefaultParsingContext("foo");
        StateSnapshot sn = ctx.snapshot();
        sn.restore();
        sn.restoreClone();
    }

    public void snapshotRestoreCloneTwice() {
        DefaultParsingContext ctx = new DefaultParsingContext("foo");
        StateSnapshot sn = ctx.snapshot();
        sn.restoreClone();
        sn.restoreClone();
    }
}
