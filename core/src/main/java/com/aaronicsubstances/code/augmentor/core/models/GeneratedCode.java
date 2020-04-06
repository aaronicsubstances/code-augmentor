package com.aaronicsubstances.code.augmentor.core.models;

import com.google.gson.annotations.SerializedName;

public class GeneratedCode {
    @SerializedName("index")
    private int index;
    @SerializedName("error")
    private boolean error;
    @SerializedName("header")
    private String headerContent;
    @SerializedName("body")
    private String bodyContent;
    @SerializedName("indent")
    private String indent;

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

    public String getHeaderContent() {
        return headerContent;
    }

    public void setHeaderContent(String headerContent) {
        this.headerContent = headerContent;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bodyContent == null) ? 0 : bodyContent.hashCode());
        result = prime * result + (error ? 1231 : 1237);
        result = prime * result + ((headerContent == null) ? 0 : headerContent.hashCode());
        result = prime * result + ((indent == null) ? 0 : indent.hashCode());
        result = prime * result + index;
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
        if (headerContent == null) {
            if (other.headerContent != null)
                return false;
        } else if (!headerContent.equals(other.headerContent))
            return false;
        if (indent == null) {
            if (other.indent != null)
                return false;
        } else if (!indent.equals(other.indent))
            return false;
        if (index != other.index)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GeneratedCode{bodyContent=" + bodyContent + ", error=" + error + ", headerContent=" + headerContent
                + ", indent=" + indent + ", index=" + index + "}";
    }
}