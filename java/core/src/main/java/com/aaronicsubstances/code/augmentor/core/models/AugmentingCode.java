package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

public class AugmentingCode {
    
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

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isStringify() {
            return stringify;
        }

        public void setStringify(boolean stringify) {
            this.stringify = stringify;
        }

        public boolean isJsonify() {
            return jsonify;
        }

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
    private String lineSeparator;
    private int nestedLevelNumber;
    private boolean hasNestedLevelStartMarker;
    private boolean hasNestedLevelEndMarker;

    // used to attach results of processing aug codes.
    // not persisted
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

    public void setId(int id) {
        this.id = id;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public String getDirectiveMarker() {
        return directiveMarker;
    }

    public void setDirectiveMarker(String directiveMarker) {
        this.directiveMarker = directiveMarker;
    }

    public String getIndent() {
        return indent;
    }

    public void setIndent(String indent) {
        this.indent = indent;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public int getNestedLevelNumber() {
        return nestedLevelNumber;
    }

    public void setNestedLevelNumber(int nestedLevelNumber) {
        this.nestedLevelNumber = nestedLevelNumber;
    }

    public List<Object> getArgs() {
        return args;
    }

    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean getHasNestedLevelStartMarker() {
        return hasNestedLevelStartMarker;
    }

    public void setHasNestedLevelStartMarker(boolean hasNestedLevelStartMarker) {
        this.hasNestedLevelStartMarker = hasNestedLevelStartMarker;
    }

    public boolean getHasNestedLevelEndMarker() {
        return hasNestedLevelEndMarker;
    }

    public void setHasNestedLevelEndMarker(boolean hasNestedLevelEndMarker) {
        this.hasNestedLevelEndMarker = hasNestedLevelEndMarker;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((args == null) ? 0 : args.hashCode());
        result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
        result = prime * result + ((directiveMarker == null) ? 0 : directiveMarker.hashCode());
        result = prime * result + (hasNestedLevelEndMarker ? 1231 : 1237);
        result = prime * result + (hasNestedLevelStartMarker ? 1231 : 1237);
        result = prime * result + id;
        result = prime * result + ((indent == null) ? 0 : indent.hashCode());
        result = prime * result + lineNumber;
        result = prime * result + ((lineSeparator == null) ? 0 : lineSeparator.hashCode());
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
        if (nestedLevelNumber != other.nestedLevelNumber)
            return false;
        if (processed != other.processed)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AugmentingCode{args=" + args + ", blocks=" + blocks + ", directiveMarker=" + directiveMarker
                + ", hasNestedLevelEndMarker=" + hasNestedLevelEndMarker + ", hasNestedLevelStartMarker="
                + hasNestedLevelStartMarker + ", id=" + id + ", indent=" + indent + ", lineNumber=" + lineNumber
                + ", lineSeparator=" + lineSeparator + ", nestedLevelNumber=" + nestedLevelNumber + ", processed="
                + processed + "}";
    }
}