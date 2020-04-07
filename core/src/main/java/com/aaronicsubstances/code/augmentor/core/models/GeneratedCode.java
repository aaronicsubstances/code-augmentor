package com.aaronicsubstances.code.augmentor.core.models;

import com.google.gson.annotations.SerializedName;

public class GeneratedCode {
    @SerializedName("index")
    private int index;
    @SerializedName("error")
    private boolean error;
    @SerializedName("body")
    private String bodyContent;
    @SerializedName("indent")
    private String indent;
    @SerializedName("skipped")
    private boolean skipped;
    @SerializedName("replace_aug_code_directives")
    private boolean replaceAugCodeDirectives;
    @SerializedName("replace_gen_code_directives")
    private boolean replaceGenCodeDirectives;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
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
        result = prime * result + ((bodyContent == null) ? 0 : bodyContent.hashCode());
        result = prime * result + (error ? 1231 : 1237);
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
        if (bodyContent == null) {
            if (other.bodyContent != null)
                return false;
        } else if (!bodyContent.equals(other.bodyContent))
            return false;
        if (error != other.error)
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
        return "GeneratedCode{bodyContent=" + bodyContent + ", error=" + error + ", indent=" + indent + ", index="
                + index + ", replaceAugCodeDirectives=" + replaceAugCodeDirectives + ", replaceGenCodeDirectives="
                + replaceGenCodeDirectives + ", skipped=" + skipped + "}";
    }
}