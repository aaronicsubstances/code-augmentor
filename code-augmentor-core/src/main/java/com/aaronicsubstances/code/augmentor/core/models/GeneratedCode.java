package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

public class GeneratedCode {

    public static class ContentPart {
        private String content;
        private boolean exactMatch;

        public ContentPart() {

        }

        public ContentPart(String content, boolean exactMatch) {
            this.content = content;
            this.exactMatch = exactMatch;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isExactMatch() {
            return exactMatch;
        }

        public void setExactMatch(boolean exactMatch) {
            this.exactMatch = exactMatch;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((content == null) ? 0 : content.hashCode());
            result = prime * result + (exactMatch ? 1231 : 1237);
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
            ContentPart other = (ContentPart) obj;
            if (content == null) {
                if (other.content != null)
                    return false;
            } else if (!content.equals(other.content))
                return false;
            if (exactMatch != other.exactMatch)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "ContentPart{content=" + content + ", exactMatch=" + exactMatch + "}";
        }
    }

    private int id;
    private List<ContentPart> contentParts;
    private String indent;
    private boolean skipped;
    private boolean replaceAugCodeDirectives;
    private boolean replaceGenCodeDirectives;
    private boolean disableEnsureEndingNewline;

    public GeneratedCode() {

    }

    public GeneratedCode(List<ContentPart> contentParts) {
        this.contentParts = contentParts;        
    }

    public GeneratedCode(int id, boolean disableEnsureEndingNewline, String indent, boolean skipped,
            boolean replaceAugCodeDirectives, boolean replaceGenCodeDirectives,            
            List<ContentPart> contentParts) {
        this.id = id;
        this.contentParts = contentParts;
        this.indent = indent;
        this.skipped = skipped;
        this.replaceAugCodeDirectives = replaceAugCodeDirectives;
        this.replaceGenCodeDirectives = replaceGenCodeDirectives;
        this.disableEnsureEndingNewline = disableEnsureEndingNewline;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIndent() {
        return indent;
    }

    public void setIndent(String indent) {
        this.indent = indent;
    }

    public boolean isDisableEnsureEndingNewline() {
        return disableEnsureEndingNewline;
    }

    public void setDisableEnsureEndingNewline(boolean disableEnsureEndingNewline) {
        this.disableEnsureEndingNewline = disableEnsureEndingNewline;
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

    public List<ContentPart> getContentParts() {
        return contentParts;
    }

    public void setContentParts(List<ContentPart> contentParts) {
        this.contentParts = contentParts;
    }

	public String getWholeContent() {
        StringBuilder wholeContent = new StringBuilder();
        for (ContentPart part : contentParts) {
            wholeContent.append(part.content);
        }
		return wholeContent.toString();
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((contentParts == null) ? 0 : contentParts.hashCode());
        result = prime * result + (disableEnsureEndingNewline ? 1231 : 1237);
        result = prime * result + id;
        result = prime * result + ((indent == null) ? 0 : indent.hashCode());
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
        if (contentParts == null) {
            if (other.contentParts != null)
                return false;
        } else if (!contentParts.equals(other.contentParts))
            return false;
        if (disableEnsureEndingNewline != other.disableEnsureEndingNewline)
            return false;
        if (id != other.id)
            return false;
        if (indent == null) {
            if (other.indent != null)
                return false;
        } else if (!indent.equals(other.indent))
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
        return "GeneratedCode{contentParts=" + contentParts + ", disableEnsureEndingNewline=" + disableEnsureEndingNewline + ", id=" + id
                + ", indent=" + indent + ", replaceAugCodeDirectives=" + replaceAugCodeDirectives
                + ", replaceGenCodeDirectives=" + replaceGenCodeDirectives + ", skipped=" + skipped + "}";
    }
}