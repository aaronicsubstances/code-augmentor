package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AugmentingCode {
    
    public static class Block {
        @SerializedName("content")
        private String content;
        @SerializedName("stringify")
        private boolean stringify;

        public Block() {
        }

        public Block(String content, boolean stringify) {
            this.content = content;
            this.stringify = stringify;
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((content == null) ? 0 : content.hashCode());
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
            if (stringify != other.stringify)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Block{content=" + content + ", stringify=" + stringify + "}";
        }
    }

    @SerializedName("index")
    private int index;
    @SerializedName("blocks")
    private List<Block> blocks;
    @SerializedName("directive_marker")
    private String directiveMarker;
    @SerializedName("indent")
    private String indent;

    public AugmentingCode() {
    }

    public AugmentingCode(List<Block> blocks) {
        this.blocks = blocks;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
        result = prime * result + ((directiveMarker == null) ? 0 : directiveMarker.hashCode());
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
        AugmentingCode other = (AugmentingCode) obj;
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
        return "AugmentingCode{blocks=" + blocks + ", directiveMarker=" + directiveMarker + ", indent=" + indent
                + ", index=" + index + "}";
    }
}