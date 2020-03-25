package com.aaronicsubstances.code.augmentor.parsing.peg;

public class NoMatchException extends Error {
    private static final long serialVersionUID = 1L;
    private final ParsingContext<?> ctx;

    public NoMatchException(ParsingContext<?> ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getMessage() {
        return ctx.getErrorDescription().toString();
    }

}
