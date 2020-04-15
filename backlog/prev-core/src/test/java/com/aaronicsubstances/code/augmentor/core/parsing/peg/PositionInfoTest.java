package com.aaronicsubstances.code.augmentor.core.parsing.peg;

import static org.testng.Assert.*;

import org.testng.annotations.*;

public class PositionInfoTest {

    @Test
    public void testGetUnderline() throws Exception {
        PositionInfo info = new PositionInfo("abc", 0);
        assertEquals(info.getUnderline('-', '*'), "*--");
        info = new PositionInfo("abc", 1);
        assertEquals(info.getUnderline('-', '*'), "-*-");
        info = new PositionInfo("abc", 2);
        assertEquals(info.getUnderline('-', '*'), "--*");
    }

    @Test
    public void testFillLineInfo() throws Exception {
        PositionInfo desc = new PositionInfo("ab\nc", 0);
        assertEquals(desc.getLine(), "ab");
        assertEquals(desc.getLineNr(), 1);
        assertEquals(desc.getIndexInLine(), 0);

        desc = new PositionInfo("ab\r\nc", 4);
        assertEquals(desc.getLine(), "c");
        assertEquals(desc.getLineNr(), 2);
        assertEquals(desc.getIndexInLine(), 0);

        desc = new PositionInfo("ab\ncd", 1);
        assertEquals(desc.getLine(), "ab");
        assertEquals(desc.getLineNr(), 1);
        assertEquals(desc.getIndexInLine(), 1);

        desc = new PositionInfo("ab\r\ncd", 1);
        assertEquals(desc.getLine(), "ab");
        assertEquals(desc.getLineNr(), 1);
        assertEquals(desc.getIndexInLine(), 1);

        desc = new PositionInfo("ab\ncd", 2);
        assertEquals(desc.getLine(), "ab");
        assertEquals(desc.getLineNr(), 1);
        assertEquals(desc.getIndexInLine(), 2);

        desc = new PositionInfo("ab\ncd", 3);
        assertEquals(desc.getLine(), "cd");
        assertEquals(desc.getLineNr(), 2);
        assertEquals(desc.getIndexInLine(), 0);

        desc = new PositionInfo("ab\ncd", 4);
        assertEquals(desc.getLine(), "cd");
        assertEquals(desc.getLineNr(), 2);
        assertEquals(desc.getIndexInLine(), 1);

        desc = new PositionInfo("ab\ncd", 5);
        assertEquals(desc.getLine(), "cd");
        assertEquals(desc.getLineNr(), 2);
        assertEquals(desc.getIndexInLine(), 2);

        desc = new PositionInfo("ab\r\ncd", 5);
        assertEquals(desc.getLine(), "cd");
        assertEquals(desc.getLineNr(), 2);
        assertEquals(desc.getIndexInLine(), 1);

        desc = new PositionInfo("漢字", 1);
        assertEquals(desc.getLine(), "漢字");
        assertEquals(desc.getLineNr(), 1);
        assertEquals(desc.getIndexInLine(), 1);
        assertEquals(desc.getUnderline('-', '*'), "-*");
    }
}
