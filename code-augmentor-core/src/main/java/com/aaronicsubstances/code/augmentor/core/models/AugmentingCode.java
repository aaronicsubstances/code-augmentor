package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

/**
 * Represents contents of an augmenting code section.
 */
public class AugmentingCode {

    /**
     * Represents a contiguous block of lines in an
     * augmenting code section which is either an
     * embedded string section, an embedded JSON
     * section, or not an embedded section. A block can
     * be of only one of these kinds.
     * <p>
     * In general a block of augmenting code is to be
     * interpreted as programming language codes to be evaluated
     * dynamically during processing stage. With data driven 
     * programming paradigm, the first block (which cannot be 
     * an embedded string or JSON data) is interpreted as the name of a function,
     * and subsequent augmenting code blocks are ignored. Embedded
     * string and JSON blocks then provide the arguments to 
     * pass to the function.
     * <p>
     * An embedded string block means that the processing stage
     * should treat it as a literal string value.
     * <p>
     * An embedded JSON block means that the processing stage should
     * treat it as a JSON value.
     */
    public static class Block {
        private String content;
        private boolean stringify;
        private boolean jsonify;

        public Block() {
        }

        public Block(String content, boolean stringify, boolean jsonify) {
            this.content = content;
            this.stringify = stringify;
            this.jsonify = jsonify;
        }

        public String getContent() {
            return content;
        }

        /**
         * Sets the content of a block/subsection of
         * an augmenting code section.
         * @param content
         */
        public void setContent(String content) {
            this.content = content;
        }

        public boolean isStringify() {
            return stringify;
        }

        /**
         * Sets the property identifying an augmenting code
         * section portion as embedded string data.
         * @param stringify true if this block is an embedded string,
         * false if it is not.
         */
        public void setStringify(boolean stringify) {
            this.stringify = stringify;
        }

        public boolean isJsonify() {
            return jsonify;
        }

        /**
         * Sets the property identifying a block/subsection of
         * augmenting code section as embedded JSON value.
         * @param jsonify true if this block is an embedded JSON value,
         * false if it is not.
         */
        public void setJsonify(boolean jsonify) {
            this.jsonify = jsonify;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((content == null) ? 0 : content.hashCode());
            result = prime * result + (jsonify ? 1231 : 1237);
            result = prime * result + (stringify ? 1231 : 1237);
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
            Block other = (Block) obj;
            if (content == null) {
                if (other.content != null)
                    return false;
            } else if (!content.equals(other.content))
                return false;
            if (jsonify != other.jsonify)
                return false;
            if (stringify != other.stringify)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Block{content=" + content + ", jsonify=" + jsonify + ", stringify=" + stringify + "}";
        }
    }

    private int id;
    private List<Block> blocks;
    private String directiveMarker;
    private String indent;
    private int lineNumber;
    private int endLineNumber;
    private String lineSeparator;
    private int nestedLevelNumber;
    private boolean hasNestedLevelStartMarker;
    private boolean hasNestedLevelEndMarker;
    private Integer matchingNestedLevelStartMarkerIndex;
    private Integer matchingNestedLevelEndMarkerIndex;
    private String externalNestedContent;
    private String genCodeIndent;
    private Integer genCodeLineNumber;
    private Integer genCodeEndLineNumber;

    // used to attach results of processing aug codes.
    private transient List<Object> args;
    private transient boolean processed;

    public AugmentingCode() {
    }

    public AugmentingCode(List<Block> blocks) {
        this.blocks = blocks;
    }

    public int getId() {
        return id;
    }

    /**
     * Sets the positive integer uniquely identifying an
     * augmenting code section in its file.
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    /**
     * Sets the blocks an augmenting code section is made up of.
     * @param blocks
     */
    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public String getDirectiveMarker() {
        return directiveMarker;
    }

    /**
     * Sets the text of the directive used to identify the
     * first block of an augmenting code section.
     * @param directiveMarker
     */
    public void setDirectiveMarker(String directiveMarker) {
        this.directiveMarker = directiveMarker;
    }

    public String getIndent() {
        return indent;
    }

    /**
     * Sets the whitespace prefix of the lines of an augmenting code
     * section with the minimum length.
     * @param indent
     */
    public void setIndent(String indent) {
        this.indent = indent;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the line number of the first line of an augmenting
     * code section. Line numbers are positive.
     * @param lineNumber
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getEndLineNumber() {
        return endLineNumber;
    }

    /**
     * Sets the line number of the last line of an augmenting
     * code section. Line numbers are positive.
     * @param endLineNumber
     */
    public void setEndLineNumber(int endLineNumber) {
        this.endLineNumber = endLineNumber;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets the line terminator of the first line of an
     * augmenting code section. Every line of an augmenting
     * code section must end with a new line.
     * @param lineSeparator the newline ending the first line of
     * this augmenting code section.
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public int getNestedLevelNumber() {
        return nestedLevelNumber;
    }

    /**
     * Sets the nested level number assigned to an
     * augmenting code section. Nested level numbers start from 0
     * and are increased by 1 after a augmenting code section
     * with a nested level start marker is encountered. It is 
     * decreased by 1 upon encountering an augmenting code section
     * with a nested level end marker.
     * @param nestedLevelNumber
     */
    public void setNestedLevelNumber(int nestedLevelNumber) {
        this.nestedLevelNumber = nestedLevelNumber;
    }

    public List<Object> getArgs() {
        return args;
    }

    /**
     * This property is not serialized into files, and is
     * only available during processing stage. It is used to
     * conveniently provide processing stage functions
     * with the embedded string or JSON blocks, in which JSON blocks are parsed. 
     * @param args 
     */
    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public boolean isProcessed() {
        return processed;
    }

    /**
     * This property is not serialized into files, and is only available
     * during processing stage. When processing stage encounters this property
     * set to true it skips over the augmenting code and does not pass it to
     * any processing stage function. This property is also used internally
     * during processing stage function to skip augmenting code objects
     * which have been processed ahead of iteration time by processing stage scripts.
     * 
     * @param processed
     */
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean getHasNestedLevelStartMarker() {
        return hasNestedLevelStartMarker;
    }

    /**
     * Sets the property that identifies an augmenting code
     * section as a start of a new nested level.
     * @param hasNestedLevelStartMarker true if this augmenting code
     * section starts a nested level; false if it does not.
     */
    public void setHasNestedLevelStartMarker(boolean hasNestedLevelStartMarker) {
        this.hasNestedLevelStartMarker = hasNestedLevelStartMarker;
    }

    public boolean getHasNestedLevelEndMarker() {
        return hasNestedLevelEndMarker;
    }

    /**
     * Sets the property that identifies an augmenting code section as
     * an end to the current nested level.
     * @param hasNestedLevelEndMarker true if this augmenting code section
     * ends current nested level; false if it does not.
     */
    public void setHasNestedLevelEndMarker(boolean hasNestedLevelEndMarker) {
        this.hasNestedLevelEndMarker = hasNestedLevelEndMarker;
    }

    public Integer getMatchingNestedLevelStartMarkerIndex() {
        return matchingNestedLevelStartMarkerIndex;
    }

    /**
     * This property is applicable only to augmenting code sections which
     * have nested level end markers. It provides convenience to 
     * processing stage by setting the index of the corresponding
     * augmenting code section in containing file which started the nested level that is
     * being ended.
     * @param matchingNestedLevelStartMarkerIndex index of augmenting code section
     * in the same file which started the nested level this object is ending;
     * null if not applicable.
     */
    public void setMatchingNestedLevelStartMarkerIndex(Integer matchingNestedLevelStartMarkerIndex) {
        this.matchingNestedLevelStartMarkerIndex = matchingNestedLevelStartMarkerIndex;
    }

    public Integer getMatchingNestedLevelEndMarkerIndex() {
        return matchingNestedLevelEndMarkerIndex;
    }

    /**
     * This property is applicable only to augmenting code sections which
     * have nested level start markers. It provides convenience to 
     * processing stage by setting the index of the corresponding
     * augmenting code section in containing file which ends the nested level that has
     * being started.
     * @param matchingNestedLevelEndMarkerIndex index of augmenting code section
     * in the same file which ends the nested level this object is starting;
     * null if not applicable.
     */
    public void setMatchingNestedLevelEndMarkerIndex(Integer matchingNestedLevelEndMarkerIndex) {
        this.matchingNestedLevelEndMarkerIndex = matchingNestedLevelEndMarkerIndex;
    }

    public String getExternalNestedContent() {
        return externalNestedContent;
    }

    /**
     * Sets the portion of the file this augmenting code section belongs to
     * from the end of this augmenting code, to just before the start of the augmenting
     * code section which ends the nested level started by this object.
     * For other objects not starting a nested level, set to null.
     * @param externalNestedContent
     */
    public void setExternalNestedContent(String externalNestedContent) {
        this.externalNestedContent = externalNestedContent;
    }

    public String getGenCodeIndent() {
        return genCodeIndent;
    }

    /**
     * Sets the whitespace prefix of the lines of the generated code section corresponding
     * to an augmenting code section which has the shortest length. If augmenting code
     * section does not currently have a generated code section, set to null.
     * @param genCodeIndent
     */
    public void setGenCodeIndent(String genCodeIndent) {
        this.genCodeIndent = genCodeIndent;
    }

    public Integer getGenCodeLineNumber() {
        return genCodeLineNumber;
    }

    /**
     * Sets the line number of the first line of the generated code
     * section existing for this augmenting code object. Set to null
     * if no generated code section exists yet.
     * @param genCodeLineNumber
     */
    public void setGenCodeLineNumber(Integer genCodeLineNumber) {
        this.genCodeLineNumber = genCodeLineNumber;
    }

    public Integer getGenCodeEndLineNumber() {
        return genCodeEndLineNumber;
    }

    /**
     * Sets the line number of the last line of the generated code
     * section existing for this augmenting code object. Set to null
     * if no generated code section exists yet.
     * @param genCodeEndLineNumber
     */
    public void setGenCodeEndLineNumber(Integer genCodeEndLineNumber) {
        this.genCodeEndLineNumber = genCodeEndLineNumber;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((args == null) ? 0 : args.hashCode());
        result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
        result = prime * result + ((directiveMarker == null) ? 0 : directiveMarker.hashCode());
        result = prime * result + endLineNumber;
        result = prime * result + ((externalNestedContent == null) ? 0 : externalNestedContent.hashCode());
        result = prime * result + ((genCodeEndLineNumber == null) ? 0 : genCodeEndLineNumber.hashCode());
        result = prime * result + ((genCodeIndent == null) ? 0 : genCodeIndent.hashCode());
        result = prime * result + ((genCodeLineNumber == null) ? 0 : genCodeLineNumber.hashCode());
        result = prime * result + (hasNestedLevelEndMarker ? 1231 : 1237);
        result = prime * result + (hasNestedLevelStartMarker ? 1231 : 1237);
        result = prime * result + id;
        result = prime * result + ((indent == null) ? 0 : indent.hashCode());
        result = prime * result + lineNumber;
        result = prime * result + ((lineSeparator == null) ? 0 : lineSeparator.hashCode());
        result = prime * result
                + ((matchingNestedLevelEndMarkerIndex == null) ? 0 : matchingNestedLevelEndMarkerIndex.hashCode());
        result = prime * result
                + ((matchingNestedLevelStartMarkerIndex == null) ? 0 : matchingNestedLevelStartMarkerIndex.hashCode());
        result = prime * result + nestedLevelNumber;
        result = prime * result + (processed ? 1231 : 1237);
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
        AugmentingCode other = (AugmentingCode) obj;
        if (args == null) {
            if (other.args != null)
                return false;
        } else if (!args.equals(other.args))
            return false;
        if (blocks == null) {
            if (other.blocks != null)
                return false;
        } else if (!blocks.equals(other.blocks))
            return false;
        if (directiveMarker == null) {
            if (other.directiveMarker != null)
                return false;
        } else if (!directiveMarker.equals(other.directiveMarker))
            return false;
        if (endLineNumber != other.endLineNumber)
            return false;
        if (externalNestedContent == null) {
            if (other.externalNestedContent != null)
                return false;
        } else if (!externalNestedContent.equals(other.externalNestedContent))
            return false;
        if (genCodeEndLineNumber == null) {
            if (other.genCodeEndLineNumber != null)
                return false;
        } else if (!genCodeEndLineNumber.equals(other.genCodeEndLineNumber))
            return false;
        if (genCodeIndent == null) {
            if (other.genCodeIndent != null)
                return false;
        } else if (!genCodeIndent.equals(other.genCodeIndent))
            return false;
        if (genCodeLineNumber == null) {
            if (other.genCodeLineNumber != null)
                return false;
        } else if (!genCodeLineNumber.equals(other.genCodeLineNumber))
            return false;
        if (hasNestedLevelEndMarker != other.hasNestedLevelEndMarker)
            return false;
        if (hasNestedLevelStartMarker != other.hasNestedLevelStartMarker)
            return false;
        if (id != other.id)
            return false;
        if (indent == null) {
            if (other.indent != null)
                return false;
        } else if (!indent.equals(other.indent))
            return false;
        if (lineNumber != other.lineNumber)
            return false;
        if (lineSeparator == null) {
            if (other.lineSeparator != null)
                return false;
        } else if (!lineSeparator.equals(other.lineSeparator))
            return false;
        if (matchingNestedLevelEndMarkerIndex == null) {
            if (other.matchingNestedLevelEndMarkerIndex != null)
                return false;
        } else if (!matchingNestedLevelEndMarkerIndex.equals(other.matchingNestedLevelEndMarkerIndex))
            return false;
        if (matchingNestedLevelStartMarkerIndex == null) {
            if (other.matchingNestedLevelStartMarkerIndex != null)
                return false;
        } else if (!matchingNestedLevelStartMarkerIndex.equals(other.matchingNestedLevelStartMarkerIndex))
            return false;
        if (nestedLevelNumber != other.nestedLevelNumber)
            return false;
        if (processed != other.processed)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AugmentingCode{args=" + args + ", blocks=" + blocks + ", directiveMarker=" + directiveMarker
                + ", endLineNumber=" + endLineNumber + ", externalNestedContent=" + externalNestedContent
                + ", genCodeEndLineNumber=" + genCodeEndLineNumber + ", genCodeIndent=" + genCodeIndent
                + ", genCodeLineNumber=" + genCodeLineNumber + ", hasNestedLevelEndMarker=" + hasNestedLevelEndMarker
                + ", hasNestedLevelStartMarker=" + hasNestedLevelStartMarker + ", id=" + id + ", indent=" + indent
                + ", lineNumber=" + lineNumber + ", lineSeparator=" + lineSeparator
                + ", matchingNestedLevelEndMarkerIndex=" + matchingNestedLevelEndMarkerIndex
                + ", matchingNestedLevelStartMarkerIndex=" + matchingNestedLevelStartMarkerIndex
                + ", nestedLevelNumber=" + nestedLevelNumber + ", processed=" + processed + "}";
    }
}