package com.aaronicsubstances.code.augmentor.core.util;

public class Token {
    public static final int DIRECTIVE_TYPE_SKIP_CODE_START = 1;
    public static final int DIRECTIVE_TYPE_SKIP_CODE_END = 2;
    public static final int DIRECTIVE_TYPE_EMB_STRING = 3;
    public static final int DIRECTIVE_TYPE_EMB_JSON = 4;
    public static final int DIRECTIVE_TYPE_AUG_CODE = 7;

    public static final int TYPE_BLANK = 20;
    public static final int TYPE_OTHER = 50;

    public int type;
    public String text;
    public int startPos;
    public int endPos;
    public int lineNumber;
    public int index;

    public int augCodeSpecIndex;
    public String indent;
    public String directiveMarker;
    public String directiveContent;
    public String newline;
    public boolean isGeneratedCodeMarker;

    /**
     * Used for JSON serialization.
     */
    public Token() {

    }

    public Token(int type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + augCodeSpecIndex;
        result = prime * result + ((directiveContent == null) ? 0 : directiveContent.hashCode());
        result = prime * result + ((directiveMarker == null) ? 0 : directiveMarker.hashCode());
        result = prime * result + endPos;
        result = prime * result + ((indent == null) ? 0 : indent.hashCode());
        result = prime * result + index;
        result = prime * result + (isGeneratedCodeMarker ? 1231 : 1237);
        result = prime * result + lineNumber;
        result = prime * result + ((newline == null) ? 0 : newline.hashCode());
        result = prime * result + startPos;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + type;
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
        Token other = (Token) obj;
        if (augCodeSpecIndex != other.augCodeSpecIndex)
            return false;
        if (directiveContent == null) {
            if (other.directiveContent != null)
                return false;
        } else if (!directiveContent.equals(other.directiveContent))
            return false;
        if (directiveMarker == null) {
            if (other.directiveMarker != null)
                return false;
        } else if (!directiveMarker.equals(other.directiveMarker))
            return false;
        if (endPos != other.endPos)
            return false;
        if (indent == null) {
            if (other.indent != null)
                return false;
        } else if (!indent.equals(other.indent))
            return false;
        if (index != other.index)
            return false;
        if (isGeneratedCodeMarker != other.isGeneratedCodeMarker)
            return false;
        if (lineNumber != other.lineNumber)
            return false;
        if (newline == null) {
            if (other.newline != null)
                return false;
        } else if (!newline.equals(other.newline))
            return false;
        if (startPos != other.startPos)
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Token{augCodeSpecIndex=" + augCodeSpecIndex + ", directiveContent=" + directiveContent
                + ", directiveMarker=" + directiveMarker + ", endPos=" + endPos + ", indent=" + indent + ", index="
                + index + ", isGeneratedCodeMarker=" + isGeneratedCodeMarker + ", lineNumber=" + lineNumber
                + ", newline=" + newline + ", startPos=" + startPos + ", text=" + text + ", type=" + type + "}";
    }
}