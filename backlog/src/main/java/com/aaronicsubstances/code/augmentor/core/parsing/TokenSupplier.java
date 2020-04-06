package com.aaronicsubstances.code.augmentor.core.parsing;

import java.util.List;

public interface TokenSupplier {
    ParserInputSource getInputSource();
    List<Token> parse();
}
