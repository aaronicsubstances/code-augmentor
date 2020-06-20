package com.aaronicsubstances.code.augmentor.core.util;

/**
 * Result of tokenizing file with {@link SourceCodeTokenizer}.
 * <p>
 * A token is a line with data associated with it for classifying it.
 */
public class Token {
    /**
     * Indicates that a token line starts a skip code section,
     * starts a generated code section, or is an inline generated
     * code section line.
     */
    public static final int DIRECTIVE_TYPE_SKIP_CODE_START = 1;

    /**
     * Indicates that a token line ends a skip code section or
     * a generated code section.
     */
    public static final int DIRECTIVE_TYPE_SKIP_CODE_END = 2;

    /**
     * Indicates embedded string line.
     */
    public static final int DIRECTIVE_TYPE_EMB_STRING = 3;
    
    /**
     * Indicates embedded JSON line.
     */
    public static final int DIRECTIVE_TYPE_EMB_JSON = 4;

    /**
     * Indicates augmenting code section line other than
     * embedded string or embedded JSON line.
     */
    public static final int DIRECTIVE_TYPE_AUG_CODE = 7;

    /**
     * Indicates line consisting of only whitespace.
     */
    public static final int TYPE_BLANK = 20;
    
    /**
     * Indicates line which does not start with a directive, and
     * has non whitespace character.
     */
    public static final int TYPE_OTHER = 50;

    /**
     * type of token line
     */
    public int type;

    /**
     * full text of token line including any ending newline.
     */
    public String text;

    /**
     * offset of token line
     */
    public int startPos;

    /**
     * offset after end of token line.
     */
    public int endPos;

    /**
     * Token line number. Starts from 1.
     */
    public int lineNumber;

    /**
     * Used to store index of token in tokens parsed from a file.
     */
    public int index;

    /**
     * Applies only to augmenting code token lines to indicate the
     * index of the directive set of which the directive of the augmenting
     * code line is part.
     */
    public int augCodeSpecIndex;

    /**
     * Leading whitespace indent of token line. Null if line consists of whitespace only.
     */
    public String indent;

    /**
     * If token line has a directive as prefix, this field stores
     * the text of that directive.
     */
    public String directiveMarker;

    /**
     * Stores substring of line after directive text, after
     * nested level marker, and before ending newline.
     */
    public String directiveContent;

    /**
     * Ending newline of token line or null if line is the last line 
     * of a file which doesn't end with a newline.
     */
    public String newline;

    /**
     * Identifies use of generated code directive in a
     * skip code directive token line.
     */
    public boolean isGeneratedCodeMarker;

    /**
     * Identifies use of inline generated code directive in a
     * skip code directive token line.
     */
    public boolean isInlineGeneratedCodeMarker;

    /**
     * Stores any nested level start marker found in token line starting
     * augmenting code section.
     */
    public String nestedLevelStartMarker;
    
    /**
     * Stores any nested level end marker found in token line starting
     * augmenting code section.
     */
    public String nestedLevelEndMarker;

    /**
     * Available for use during prepare stage of Code Augmentor to store
     * nested level numbers of augmenting code token lines.
     */
    public int nestedLevelNumber;

    /**
     * Used for JSON serialization.
     */
    public Token() {

    }

    /**
     * Constructs token with required type of the token
     * @param type token type
     */
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
        result = prime * result + (isInlineGeneratedCodeMarker ? 1231 : 1237);
        result = prime * result + lineNumber;
        result = prime * result + ((nestedLevelEndMarker == null) ? 0 : nestedLevelEndMarker.hashCode());
        result = prime * result + nestedLevelNumber;
        result = prime * result + ((nestedLevelStartMarker == null) ? 0 : nestedLevelStartMarker.hashCode());
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
        if (isInlineGeneratedCodeMarker != other.isInlineGeneratedCodeMarker)
            return false;
        if (lineNumber != other.lineNumber)
            return false;
        if (nestedLevelEndMarker == null) {
            if (other.nestedLevelEndMarker != null)
                return false;
        } else if (!nestedLevelEndMarker.equals(other.nestedLevelEndMarker))
            return false;
        if (nestedLevelNumber != other.nestedLevelNumber)
            return false;
        if (nestedLevelStartMarker == null) {
            if (other.nestedLevelStartMarker != null)
                return false;
        } else if (!nestedLevelStartMarker.equals(other.nestedLevelStartMarker))
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
                + index + ", isGeneratedCodeMarker=" + isGeneratedCodeMarker + ", isInlineGeneratedCodeMarker="
                + isInlineGeneratedCodeMarker + ", lineNumber=" + lineNumber + ", nestedLevelEndMarker="
                + nestedLevelEndMarker + ", nestedLevelNumber=" + nestedLevelNumber + ", nestedLevelStartMarker="
                + nestedLevelStartMarker + ", newline=" + newline + ", startPos=" + startPos + ", text=" + text
                + ", type=" + type + "}";
    }
}