package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

/**
 * Represents generated code objects which constitutes the contents of
 * the output file of the processing stage.
 */
public class GeneratedCode {

    /**
     * Represents a section of generated code content. The purpose of this
     * class is to be able to specify sections of generated code content which
     * should remain intact and not be changed during indentation of code
     * in completion stage of Code Augmentor. This is because 
     * during indentation a leading indent may be inserted in a section of
     * generated code.
     */
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

        /**
         * Sets property which exempts content from any kind of permissible
         * modification such as by indentation.
         * @param exactMatch true to exempt content from indentation; 
         * false to let the possibility of applying leading indent remain.
         */
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

    /**
     * Sets the id of the augmenting code object to which this generated code
     * object corresponds.
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    public String getIndent() {
        return indent;
    }

    /**
     * Sets the indent to apply to the generated code object. If empty string,
     * then no indentation will be applied. If null, then by default generated code section's
     * indent will be applied if that section exists with an indent. 
     * If no indent could be found from a generated code section, then
     * as a last resort indent of augmenting code section will be used.
     * @param indent
     */
    public void setIndent(String indent) {
        this.indent = indent;
    }

    public boolean isDisableEnsureEndingNewline() {
        return disableEnsureEndingNewline;
    }

    /**
     * Sets the property which prevents appending newline to generated code
     * content not ending with one. By default completion stage ensures a 
     * generated code ends with a newline by adding one when no trailing newline
     * is found.
     * @param disableEnsureEndingNewline
     */
    public void setDisableEnsureEndingNewline(boolean disableEnsureEndingNewline) {
        this.disableEnsureEndingNewline = disableEnsureEndingNewline;
    }

    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Sets the property which communicates to completion stage that it should skip
     * getting and merging generated code for an augmenting code section. If
     * a file has augmenting codes, and all of them are skipped, then completion stage
     * will never generate a file for it even if code change detection is disabled. This
     * feature enables one to supply non-source files such as config files to processing stage,
     * to set up global and file scopes of helper context object and perform some initialization.
     * @param skipped
     */
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isReplaceAugCodeDirectives() {
        return replaceAugCodeDirectives;
    }

    /**
     * Sets property which directs completion stage to include augmenting code
     * section itself in the range of source file to be replaced by generated
     * code section. By default only generated code sections are targeted for
     * replacement.
     * @param replaceAugCodeDirectives
     */
    public void setReplaceAugCodeDirectives(boolean replaceAugCodeDirectives) {
        this.replaceAugCodeDirectives = replaceAugCodeDirectives;
    }

    public boolean isReplaceGenCodeDirectives() {
        return replaceGenCodeDirectives;
    }

    /**
     * Sets property which directs completion stage to include generated code
     * section itself in the range of source file to be replaced by generated
     * code section. This resembles the default in which only generated code sections are
     * targeted for replacement. It is different in the following way.
     * <ul>
     *  <li>It can however be combined with {@link #setReplaceAugCodeDirectives(boolean)}.
     *  <li>It can also be used to prevent default wrapping of generated code content with
     * generated code start/end directives. If using inline generated code directives,
     * then this is the property that must be used to allow processing stage to dictate
     * how generated code should wrapped.
     *  <li>The range implied by this property includes the blank lines allowable between
     * an augmenting code section and its generated code section.
     * </ul>
     * @param replaceGenCodeDirectives
     */
    public void setReplaceGenCodeDirectives(boolean replaceGenCodeDirectives) {
        this.replaceGenCodeDirectives = replaceGenCodeDirectives;
    }

    public List<ContentPart> getContentParts() {
        return contentParts;
    }

    /**
     * Sets the parts of a generated code content.
     */
    public void setContentParts(List<ContentPart> contentParts) {
        this.contentParts = contentParts;
    }

    /**
     * Gets generated content concatenated from content parts.
     * @return combined generated code content
     */
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