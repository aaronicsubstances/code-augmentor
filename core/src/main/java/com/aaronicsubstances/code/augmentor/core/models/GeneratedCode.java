package com.aaronicsubstances.code.augmentor.core.models;

public class GeneratedCode {
    private int index;
    private String content;
    private String indent;
    private boolean skipped;
    private boolean replaceAugCodeDirectives;
    private boolean replaceGenCodeDirectives;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIndent() {
        return indent;
    }

    public void setIndent(String indent) {
        this.indent = indent;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isReplaceAugCodeDirectives() {
        return replaceAugCodeDirectives;
    }

    public void setReplaceAugCodeDirectives(boolean replaceAugCodeDirectives) {
        this.replaceAugCodeDirectives = replaceAugCodeDirectives;
    }

    public boolean isReplaceGenCodeDirectives() {
        return replaceGenCodeDirectives;
    }

    public void setReplaceGenCodeDirectives(boolean replaceGenCodeDirectives) {
        this.replaceGenCodeDirectives = replaceGenCodeDirectives;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((content == null) ? 0 : content.hashCode());
        result = prime * result + ((indent == null) ? 0 : indent.hashCode());
        result = prime * result + index;
        result = prime * result + (replaceAugCodeDirectives ? 1231 : 1237);
        result = prime * result + (replaceGenCodeDirectives ? 1231 : 1237);
        result = prime * result + (skipped ? 1231 : 1237);
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
        GeneratedCode other = (GeneratedCode) obj;
        if (content == null) {
            if (other.content != null)
                return false;
        } else if (!content.equals(other.content))
            return false;
        if (indent == null) {
            if (other.indent != null)
                return false;
        } else if (!indent.equals(other.indent))
            return false;
        if (index != other.index)
            return false;
        if (replaceAugCodeDirectives != other.replaceAugCodeDirectives)
            return false;
        if (replaceGenCodeDirectives != other.replaceGenCodeDirectives)
            return false;
        if (skipped != other.skipped)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GeneratedCode{content=" + content + ", indent=" + indent + ", index="
                + index + ", replaceAugCodeDirectives=" + replaceAugCodeDirectives + ", replaceGenCodeDirectives="
                + replaceGenCodeDirectives + ", skipped=" + skipped + "}";
    }
}