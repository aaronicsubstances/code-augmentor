package com.aaronicsubstances.code.augmentor.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SourceCodeTokenizer {    

    private static class DirectiveDescriptor implements Comparable<DirectiveDescriptor> {
        final String marker;
        final DirectiveDescriptorType type;
        final int augCodeIndex;
        final boolean isNestedLevelStart;
        
        public DirectiveDescriptor(String marker, int augCodeIndex) {
            this.marker = marker;
            type = DirectiveDescriptorType.AUG_CODE;
            this.augCodeIndex = augCodeIndex;
            isNestedLevelStart = false;
        }

        public DirectiveDescriptor(String marker, boolean isNestedLevelStart) {
            this.marker = marker;
            type = DirectiveDescriptorType.NESTED_LEVEL_MARKER;
            augCodeIndex = 0;
            this.isNestedLevelStart = isNestedLevelStart;
        }

        public DirectiveDescriptor(String marker, DirectiveDescriptorType type) {
            this.marker = marker;
            this.type = type;
            augCodeIndex = 0;
            isNestedLevelStart = false;
        }

        @Override
        public int compareTo(DirectiveDescriptor o) {
            return this.marker.compareTo(o.marker);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((marker == null) ? 0 : marker.hashCode());
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
            DirectiveDescriptor other = (DirectiveDescriptor) obj;
            if (marker == null) {
                if (other.marker != null)
                    return false;
            } else if (!marker.equals(other.marker))
                return false;
            return true;
        }
    }

    private enum DirectiveDescriptorType {
        GEN_CODE_START, GEN_CODE_END, INLINE_GEN_CODE,
        SKIP_CODE_START, SKIP_CODE_END,
        EMB_STR, EMB_JSON, AUG_CODE,
        NESTED_LEVEL_MARKER
    }

    private final List<DirectiveDescriptor> directiveDescriptors = new ArrayList<>();
    private final List<DirectiveDescriptor> nestedLevelMarkerDescriptors = new ArrayList<>();

    private final Pattern lexerRegex;
    private final Pattern nestedLevelMarkerRegex;

    public SourceCodeTokenizer(
            List<String> genCodeStartDirectives,
            List<String> genCodeEndDirectives,
            List<String> embeddedStringDirectives,
            List<String> embeddedJsonDirectives,
            List<String> skipCodeStartDirectives, 
            List<String> skipCodeEndDirectives,
            List<List<String>> augCodeDirectiveSets, 
            List<String> inlineGenCodeDirectives, 
            List<String> nestedLevelStartMarkers, 
            List<String> nestedLevelEndMarkers) {       

        // aug code directives are the only required directives.
        for (int i = 0; i < augCodeDirectiveSets.size(); i++) {
            for (String marker: augCodeDirectiveSets.get(i)) {
                DirectiveDescriptor d = new DirectiveDescriptor(marker, i);
                directiveDescriptors.add(d);
            }
        }

        if (genCodeStartDirectives != null) {
            for (String marker: genCodeStartDirectives) {
                directiveDescriptors.add(new DirectiveDescriptor(marker, 
                    DirectiveDescriptorType.GEN_CODE_START));
            }
        }

        if (genCodeEndDirectives != null) {
            for (String marker: genCodeEndDirectives) {
                directiveDescriptors.add(new DirectiveDescriptor(marker, 
                    DirectiveDescriptorType.GEN_CODE_END));
            }
        }

        if (inlineGenCodeDirectives != null) {
            for (String marker: inlineGenCodeDirectives) {
                directiveDescriptors.add(new DirectiveDescriptor(marker, 
                    DirectiveDescriptorType.INLINE_GEN_CODE));
            }
        }

        if (skipCodeStartDirectives != null) {
            for (String marker: skipCodeStartDirectives) {
                directiveDescriptors.add(new DirectiveDescriptor(marker, 
                    DirectiveDescriptorType.SKIP_CODE_START));
            }
        }

        if (skipCodeEndDirectives != null) {
            for (String marker: skipCodeEndDirectives) {
                directiveDescriptors.add(new DirectiveDescriptor(marker, 
                    DirectiveDescriptorType.SKIP_CODE_END));
            }
        }

        if (embeddedStringDirectives != null) {
            for (String marker: embeddedStringDirectives) {
                directiveDescriptors.add(new DirectiveDescriptor(marker,
                    DirectiveDescriptorType.EMB_STR));
            }
        }

        if (embeddedJsonDirectives != null) {
            for (String marker: embeddedJsonDirectives) {
                directiveDescriptors.add(new DirectiveDescriptor(marker,
                    DirectiveDescriptorType.EMB_JSON));
            }
        }

        directiveDescriptors.removeIf(d -> TaskUtils.isBlank(d.marker));
        Collections.sort(directiveDescriptors);

        String lexerRegexBuilder = join("|", directiveDescriptors.stream()
            .map(x -> x.marker)
            // let longer strings appear first to ensure if a directive is a prefix
            // of another, the latter and longer one is chosen.
            .sorted((x, y) -> Integer.compare(y.length(), x.length()))
            .map(x -> Pattern.quote(x)));

        // build regex to allow for leading whitespace before directives
        lexerRegex = Pattern.compile("^\\s*(" + lexerRegexBuilder + ")");

        if (nestedLevelStartMarkers != null) {
            for (String marker: nestedLevelStartMarkers) {
                nestedLevelMarkerDescriptors.add(new DirectiveDescriptor(marker,
                    true));
            }
        }

        if (nestedLevelEndMarkers != null) {
            for (String marker: nestedLevelEndMarkers) {
                nestedLevelMarkerDescriptors.add(new DirectiveDescriptor(marker,
                    false));
            }
        }

        nestedLevelMarkerDescriptors.removeIf(d -> TaskUtils.isBlank(d.marker));
        Collections.sort(nestedLevelMarkerDescriptors);

        String nestedLevelMarkerRegexBuilder = join("|", nestedLevelMarkerDescriptors.stream()
            .map(x -> x.marker)
            // let longer strings appear first to ensure if a marker is a prefix
            // of another, the latter and longer one is chosen.
            .sorted((x, y) -> Integer.compare(y.length(), x.length()))
            .map(x -> Pattern.quote(x)));
        nestedLevelMarkerRegex = Pattern.compile("^(?:" + nestedLevelMarkerRegexBuilder + ")");
    }

    private static String join(String separator, Stream<String> stringStream) {
        StringBuilder result = new StringBuilder();
        boolean[] foundAny = new boolean[]{ false };
        stringStream.forEachOrdered(s ->{
            if (foundAny[0]) {
                result.append(separator);
            }
            result.append(s);
            foundAny[0] = true;
        });
        return result.toString();
    }
    
    public List<Token> tokenizeSource(String source) {
        List<String> splitSource = TaskUtils.splitIntoLines(source);
        List<Token> tokens = new ArrayList<>();
        int startPos = 0;
        for (int i = 0; i < splitSource.size(); i+=2) {
            String line = splitSource.get(i);
            String terminator = splitSource.get(i + 1);
            Token t = null;
            if (line.trim().isEmpty()) {
                t = new Token(Token.TYPE_BLANK);
            }
            if (t == null) {
                Matcher m = lexerRegex.matcher(line);
                if (m.find() && !TaskUtils.isEmpty(m.group(1))) {
                    String directiveMarker = m.group(1);
                    int directiveMarkerIndex = m.start(1);
                    DirectiveDescriptor directiveDescriptor = new DirectiveDescriptor(
                        directiveMarker, -1);
                    int idx = Collections.binarySearch(directiveDescriptors, 
                        directiveDescriptor);
                    directiveDescriptor = directiveDescriptors.get(idx);
                    switch (directiveDescriptor.type) {
                        case GEN_CODE_START:
                            t = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_START, directiveMarker, 
                                directiveMarkerIndex, line);
                            t.isGeneratedCodeMarker = true;                       
                            break;
                        case GEN_CODE_END:
                            t = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_END, directiveMarker, 
                                directiveMarkerIndex, line);
                            t.isGeneratedCodeMarker = true;
                            break;
                        case INLINE_GEN_CODE:
                            t = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_START, directiveMarker,
                                directiveMarkerIndex ,line);
                            t.isGeneratedCodeMarker = true;
                            t.isInlineGeneratedCodeMarker = true;
                            break;
                        case SKIP_CODE_START:
                            t = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_START, directiveMarker,
                                directiveMarkerIndex, line);
                            break;
                        case SKIP_CODE_END:
                            t = createToken(Token.DIRECTIVE_TYPE_SKIP_CODE_END, directiveMarker, 
                                directiveMarkerIndex, line);
                            break;
                        case EMB_STR:
                            t = createToken(Token.DIRECTIVE_TYPE_EMB_STRING, directiveMarker,
                                directiveMarkerIndex, line);
                            break;
                        case EMB_JSON:
                            t = createToken(Token.DIRECTIVE_TYPE_EMB_JSON, directiveMarker,
                                directiveMarkerIndex, line);
                            break;
                        default:
                            break;
                    }
                    if (t == null) {
                        assert directiveDescriptor.type == DirectiveDescriptorType.AUG_CODE;
                        t = createToken(Token.DIRECTIVE_TYPE_AUG_CODE, directiveMarker,
                            directiveMarkerIndex, line);
                        t.augCodeSpecIndex = directiveDescriptor.augCodeIndex;
                        
                        m = nestedLevelMarkerRegex.matcher(t.directiveContent);
                        if (m.find() && !TaskUtils.isEmpty(m.group())) {
                            String nestedLevelMarker = m.group();
                            DirectiveDescriptor nestedLevelMarkerDescriptor = new DirectiveDescriptor(nestedLevelMarker,
                                    -1);
                            idx = Collections.binarySearch(nestedLevelMarkerDescriptors,
                                nestedLevelMarkerDescriptor);
                            nestedLevelMarkerDescriptor = nestedLevelMarkerDescriptors.get(idx);
                            if (nestedLevelMarkerDescriptor.isNestedLevelStart) {
                                t.nestedLevelStartMarker = nestedLevelMarker;
                            }
                            else {
                                t.nestedLevelEndMarker = nestedLevelMarker;
                            }
                            t.directiveContent = t.directiveContent.substring(m.end());
                        }
                    }
                }
            }
            if (t == null) {
                // token must be of some other type.
                t = new Token(Token.TYPE_OTHER);
            }
            t.text = line;
            if (terminator != null) {
                t.text += terminator;
                t.newline = terminator;
            }
            t.startPos = startPos;
            t.endPos = startPos + t.text.length();
            // collapse (line, terminator) pair from split source
            // to single index.
            t.index = i / 2;
            t.lineNumber = t.index + 1;
            
            tokens.add(t);

            startPos = t.endPos;
        }
        return tokens;
    }

    private static Token createToken(int directiveType, String directiveMarker, 
            int dIndex, String line) {
        Token t = new Token(directiveType);
        t.directiveMarker = directiveMarker;
        t.indent = line.substring(0, dIndex);
        t.directiveContent = line.substring(dIndex + directiveMarker.length());
        return t;
    }
}