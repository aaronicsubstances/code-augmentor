package com.aaronicsubstances.code.augmentor.core.parsing.peg.extras;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.parsing.peg.NoMatchException;
import com.aaronicsubstances.code.augmentor.core.parsing.peg.Parser;
import com.aaronicsubstances.code.augmentor.core.parsing.peg.PositionInfo;
import com.aaronicsubstances.code.augmentor.core.parsing.peg.ParsingContext.ErrorDescription;

public class StackEnabledParser extends Parser<StackEnabledParsingContext> {

    public static class LogData {
        public final int depth;
        public final boolean supressLogs;
    
        public LogData(int depth, boolean supressLogs) {
            this.depth = depth;
            this.supressLogs = supressLogs;
        }
    }

    public static class LogDecision {
        public final boolean loggingEnabled;
        public final int logDepth;
        public final boolean logBegin;
        public final boolean logEndSuccess;
        public final boolean logEndSuccessVerbosely;
        public final boolean logEndError;

        public LogDecision() {
            this(false, 0, false, false, false, false);
        }

        public LogDecision(boolean loggingEnabled, int logDepth,
                boolean logBegin, boolean logEndSuccess, 
                boolean logEndSuccessVerbosely, boolean logEndError) {
            this.loggingEnabled = loggingEnabled;
            this.logDepth = logDepth;
            this.logBegin = logBegin;
            this.logEndSuccess = logEndSuccess;
            this.logEndSuccessVerbosely = logEndSuccessVerbosely;
            this.logEndError = logEndError;
        }
    }

    private Consumer<String> traceLog;
    private boolean verbose;
    private int indentSize = 4;
    private char indentChar = ' ';

    // internal state fields.
    private final String LOG_LEVEL_NAME_TRACE = "TRACE";
    private final String LOG_LEVEL_NAME_INFO = "INFO";
    private int lastStateIndex;
    
    public StackEnabledParser(StackEnabledParsingContext ctx) {
        super(ctx);
    }

    public Consumer<String> getTraceLog() {
        return traceLog;
    }

    public void setTraceLog(Consumer<String> traceLog) {
        this.traceLog = traceLog;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    public char getIndentChar() {
        return indentChar;
    }

    public void setIndentChar(char indentChar) {
        this.indentChar = indentChar;
    }

    public List<PegToken> getTokenList() {        
        List<PegToken> tokenList = new ArrayList<>();
        Stack<Object> tokenStack = getParsingContext().getValueStackMap().get(PegToken.class);
        if (tokenStack != null) {
            for (Object stackItem : tokenStack) {
                assert stackItem instanceof PegToken;
                // looks like stack iterator presents items in FIFO order.
                tokenList.add((PegToken)stackItem);
            }
        }
        return tokenList;
    }

    public void runRule(String ruleName, Runnable ruleCode) {
        runRule(ruleName, false, ruleCode);
    }

    public void runRule(String ruleName, boolean suppressChildNodes, Runnable ruleCode) {
        LogDecision logDecision = markRuleStart(ruleName, suppressChildNodes);
        try {
            if (logDecision.logBegin) {
                beginLog(ruleName, logDecision);
            }
            ruleCode.run();
            if (logDecision.logEndSuccess) {
                endLogSuccess(ruleName, logDecision);
            }
        }
        catch (NoMatchException ex) {
            if (logDecision.logEndError) {
                endLogError(ruleName, logDecision);
            }
            throw ex;
        }
        finally {
            markRuleEnd(logDecision);
        }
    }

    private LogDecision markRuleStart(String ruleName, boolean suppressChildNodes) {
        push(getParsingContext().state().index);

        // interpret null rule name to mean no need for logging.
        if (ruleName == null) {
            return new LogDecision();
        }

        // also ignore logging if trace logger has not been provided.
        if (traceLog == null) {
            return new LogDecision();
        }

        // lastly ignore logging if suppression is in force.
        LogData currLogData = peek(LogData.class);
        if (currLogData != null && currLogData.supressLogs) {
            return new LogDecision();
        }

        if (currLogData == null) {
            currLogData = new LogData(0, false);
            push(currLogData);
        }
        int logDepth = currLogData.depth;
        LogData childLogData = new LogData(logDepth + 1, suppressChildNodes);
        push(childLogData);

        boolean logEndError = verbose;
        
        // make major log if at boundary of child node suppression.
        boolean logEndSuccessVerbosely = suppressChildNodes;

        return new LogDecision(true, logDepth, 
            true, true, logEndSuccessVerbosely, logEndError);
    }

    private void markRuleEnd(LogDecision logDecision) {
        lastStateIndex = pop(Integer.class);
        if (logDecision.loggingEnabled) {
            pop(LogData.class);
        }
    }

    private void beginLog(String ruleName, LogDecision logDecision) {
        int currPos = getParsingContext().state().index;
        String indent = createIndent(logDecision.logDepth, null);
        String log = String.format("%s[%s] Attempting to match rule '%s' at pos %s...", 
            indent, LOG_LEVEL_NAME_TRACE, ruleName, currPos);
        traceLog.accept(log);
    }

    private void endLogError(String ruleName, LogDecision logDecision) {
        int currPos = getParsingContext().state().index;
        String msgIndent = createIndent(logDecision.logDepth, LOG_LEVEL_NAME_TRACE);
        String errorMsg = craftErrorMessage(msgIndent);        
        String indent = createIndent(logDecision.logDepth, null);
        String log = String.format("%s[%s] Failed to match rule '%s' at pos %s: %s",
            indent, LOG_LEVEL_NAME_TRACE, ruleName, currPos, errorMsg);
        traceLog.accept(log);
	}

    private void endLogSuccess(String ruleName, LogDecision logDecision) {
        if (logDecision.logEndSuccessVerbosely) { 
            logMajorSuccess(ruleName, logDecision);
        }
        else {
            int currPos = getParsingContext().state().index;
            String indent = createIndent(logDecision.logDepth, null);
            String log = String.format("%s[%s] Successfully matched rule '%s' ending at pos %s.", 
                indent, LOG_LEVEL_NAME_INFO, ruleName, currPos - 1);
            traceLog.accept(log);
        }
    }

    private void logMajorSuccess(String ruleName, LogDecision logDecision) {
        String msgIndent = createIndent(logDecision.logDepth, LOG_LEVEL_NAME_INFO);
        String successMsg = craftSuccessMessage(msgIndent);
        String indent = createIndent(logDecision.logDepth, null);
        int currPos = getParsingContext().state().index;
        String log = String.format("%s[%s] Successfully matched rule '%s' ending at pos %s: %s", 
            indent, LOG_LEVEL_NAME_INFO, ruleName, currPos - 1, successMsg);
        traceLog.accept(log);
    }

    private String createIndent(int depth, String extra) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < indentSize; j++) {
                indent.append(indentChar);
            }
        }
        if (extra != null) {
            // add 3 for surrounding square brackets and extra space after closing
            // square bracket.
            for (int i = 0; i < extra.length() + 3; i++) {
                indent.append(indentChar);
            }
        }
        return indent.toString();
    }

    private String craftErrorMessage(String indent) {
        ErrorDescription errorDesc = getParsingContext().getErrorDescription();
        String expectations = errorDesc.expectations.stream()
            .collect(Collectors.joining(", "));
        return "Error on line " + errorDesc.errorLineInfo.getLineNr() + ". Expected: "
                    + expectations + " instead of " + escapeString(errorDesc.errorLineInfo.getPositionChar())
                    + "\n" + indent + errorDesc.errorLineInfo.getLine() + "\n" + indent +
                    errorDesc.errorLineInfo.getUnderline(' ', '^');
    }

    private String craftSuccessMessage(String indent) {
        int currPos = getParsingContext().state().index;
        PositionInfo currPosInfo = new PositionInfo(getParsingContext().getContent(), 
            currPos - 1);
        return "Line " + currPosInfo.getLineNr()
                    + "\n" + indent + currPosInfo.getLine() + "\n" + indent +
                    currPosInfo.getUnderline(' ', '^');

    }

    protected void push(Object item) {
        Class<?> itemClass = item.getClass();
        Map<Class<?>, Stack<Object>> valueStackMap = getParsingContext().getValueStackMap();
        Stack<Object> valueStack;
        if (valueStackMap.containsKey(itemClass)) {
            valueStack = valueStackMap.get(itemClass);
        }
        else {
            valueStack = new Stack<>();
            valueStackMap.put(itemClass, valueStack);
        }
        valueStack.push(item);
    }

    @SuppressWarnings("unchecked")
    protected <T> T peek(Class<T> itemClass) {
        Map<Class<?>, Stack<Object>> valueStackMap = getParsingContext().getValueStackMap();
        if (valueStackMap.containsKey(itemClass)) {
            Stack<Object> valueStack = valueStackMap.get(itemClass);
            T stackItem = (T)valueStack.peek();
            return stackItem;
        }
        else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T pop(Class<T> itemClass) {
        Map<Class<?>, Stack<Object>> valueStackMap = getParsingContext().getValueStackMap();
        if (valueStackMap.containsKey(itemClass)) {
            Stack<Object> valueStack = valueStackMap.get(itemClass);
            T stackItem = (T)valueStack.pop();
            return stackItem;
        }
        else {
            throw new NoSuchElementException();
        }
    }

    protected IndexRange matchRange() {
        int startPos = lastStateIndex;
        int endPos = getParsingContext().state().index;
        return new IndexRange(startPos, endPos);
    }
}