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

    private int index;
    private List<ContentPart> contentParts;
    private String indent;
    private boolean skipped;
    private boolean replaceAugCodeDirectives;
    private boolean replaceGenCodeDirectives;

    public GeneratedCode() {

    }

    public GeneratedCode(List<ContentPart> contentParts) {
        this.contentParts = contentParts;        
    }

    // Intended for use by Groovy scripts
    public ContentPart newPart(String content) {
        return new ContentPart(content, false);
    }

    // Intended for use by Groovy scripts
    public ContentPart newPart(String content, boolean exactMatch) {
        return new ContentPart(content, exactMatch);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
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
        if (contentParts == null) {
            if (other.contentParts != null)
                return false;
        } else if (!contentParts.equals(other.contentParts))
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
        return "GeneratedCode{contentParts=" + contentParts + ", indent=" + indent + ", index=" + index
                + ", replaceAugCodeDirectives=" + replaceAugCodeDirectives + ", replaceGenCodeDirectives="
                + replaceGenCodeDirectives + ", skipped=" + skipped + "}";
    }
}