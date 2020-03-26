package com.aaronicsubstances.code.augmentor.parsing.peg.extras;

import java.util.List;

public class PegToken {
	public static final String TYPE_DS_COMMENT = "DS_COMMENT";
	public static final String TYPE_SS_COMMENT = "SS_COMMENT";
    public static final String TYPE_STRING_DELIMITER = "SINQLE_QUOTE";
    public static final String TYPE_TRIPLE_QUOTED_STRING_DELIMITER = "TRIPLE_QUOTE";
    public static final String TYPE_NEWLINE = "NEWLINE";
    public static final String TYPE_NON_NEWLINE_WS = "NON_NEWLINE_WS";
    public static final String TYPE_LITERAL_STRING_CONTENT = "LITERAL_STRING_CONTENT";
    public static final String TYPE_IMPORT = "IMPORT";
    public static final String TYPE_PACKAGE = "PACKAGE";
	public static final String TYPE_QUASI_ID = "QUASI_ID";
    public static final String TYPE_OTHER = "OTHER";
	public static final String TYPE_SHEBANG = "SHEBANG";
	public static final String TYPE_STRING_TEMPLATE_START = "STRING_TEMPLATE_START";
	public static final String TYPE_STRING_TEMPLATE_END = "STRING_TEMPLATE_END";
	public static final String TYPE_BRACED_BLOCK_START = "BRACED_BLOCK_START";
	public static final String TYPE_BRACED_BLOCK_END = "BRACED_BLOCK_END";

	public final String type;
    public final int startPos;
    public final int endPos;    
    public final List<IndexRange> importStatement;

    public String input;

    public PegToken(String type, IndexRange range) {
        this(type, range, null);
    }

    public PegToken(String type, IndexRange range, List<IndexRange> importStatement) {
        this.type = type;
        this.startPos = range.start;
        this.endPos = range.end;
        this.importStatement = importStatement;
    }

    @Override
    public String toString() {
        String rangeDesc;
        if (input != null) {
            String text = input.substring(startPos, endPos);
            rangeDesc = "text=" + text;
        }
        else {
            rangeDesc = "startPos=" + startPos + ", endPos=" + endPos;
        }
        if (importStatement != null) {
            StringBuilder importStatementStr;
            if (input != null) {
                importStatementStr = new StringBuilder();
                for (IndexRange range : importStatement) {
                    String s = input.substring(range.start, range.end);
                    if ("as".equals(s)) {
                        importStatementStr.append(' ');
                    }
                    importStatementStr.append(s);
                    if ("import".equals(s) || "static".equals(s) || "as".equals(s)) {
                        importStatementStr.append(' ');
                    }
                }
            }
            else {
                importStatementStr = new StringBuilder().append(importStatement);
            }
            return "type=" + type + ", " + rangeDesc + ", " +
                "importStatement=" + importStatementStr;
        }
        else {
            return "type=" + type + ", " + rangeDesc;
        }
    }
}