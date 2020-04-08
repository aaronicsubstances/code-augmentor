package com.aaronicsubstances.code.augmentor.core.parsing;

public class Token {
    public final int type;
    public String text;
    public int startPos;
    public int endPos;
    public int lineNumber;
    public int index;

    public int directiveType;
    public int augCodeSpecIndex;
    public String indent;
    public String directiveMarker;
    public String directiveContent;
    public String newline;
	public boolean uncheckedAugCodeDirective;

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
        result = prime * result + directiveType;
        result = prime * result + endPos;
        result = prime * result + ((indent == null) ? 0 : indent.hashCode());
        result = prime * result + index;
        result = prime * result + lineNumber;
        result = prime * result + ((newline == null) ? 0 : newline.hashCode());
        result = prime * result + startPos;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + type;
        result = prime * result + (uncheckedAugCodeDirective ? 1231 : 1237);
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
        if (directiveType != other.directiveType)
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
        if (uncheckedAugCodeDirective != other.uncheckedAugCodeDirective)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Token{augCodeSpecIndex=" + augCodeSpecIndex + ", directiveContent=" + directiveContent
                + ", directiveMarker=" + directiveMarker + ", directiveType=" + directiveType + ", endPos=" + endPos
                + ", indent=" + indent + ", index=" + index + ", lineNumber=" + lineNumber + ", newline=" + newline
                + ", startPos=" + startPos + ", text=" + text + ", type=" + type + ", uncheckedAugCodeDirective="
                + uncheckedAugCodeDirective + "}";
    }
}