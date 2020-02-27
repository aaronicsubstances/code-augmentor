package com.aaronicsubstances.programmer.companion;

import java.util.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class JavaLexerTest {
	
	@Test
	public void testTokenInequality() {
		Token one = new Token();
		Token two = new Token(Token.TOKEN_TYPE_OTHER, "d", 0, 1, 1, 1);
		assertNotEquals(one, two);
	}
	
	@Test
	public void testTokenEquality() {
		Token one = new Token(Token.TOKEN_TYPE_OTHER, "ate", 7, 10, 2, 3);
		Token two = new Token(Token.TOKEN_TYPE_OTHER, "ate", 7, 10, 2, 3);
		assertEquals(one, two);
	}
	
	@ParameterizedTest
	@MethodSource
	public void testLexing(String sourceCode, List<Token> expected) {
		List<Token> actual = iteratorToList(new JavaLexer(sourceCode));
		assertEquals(expected, actual);
	}
	
	static Stream<Arguments> testLexing() {
        return Stream.of(
			Arguments.of("", Arrays.asList()),
			Arguments.of("package com.aaronicsubstances", Arrays.asList(
				new Token(Token.TOKEN_TYPE_OTHER, "package", 0, 7, 1, 1),
				new Token(Token.TOKEN_TYPE_NON_NEWLINE_WHITE_SPACE, " ", 7, 8, 1, 8),
				new Token(Token.TOKEN_TYPE_OTHER, "com.aaronicsubstances", 8, 29, 1, 9))),
			Arguments.of("//", Arrays.asList(
				new Token(Token.TOKEN_TYPE_SINGLE_LINE_COMMENT_START, "//", 0, 2, 1, 1))),			
			Arguments.of("\r\n// just a test\n", Arrays.asList(
				new Token(Token.TOKEN_TYPE_NEWLINE, "\r\n", 0, 2, 1, 1),
				new Token(Token.TOKEN_TYPE_SINGLE_LINE_COMMENT_START, "//", 2, 4, 2, 1),
				new Token(Token.TOKEN_TYPE_OTHER, " just a test", 4, 16, 2, 3),
				new Token(Token.TOKEN_TYPE_SINGLE_LINE_COMMENT_END, null, 16, 16, 2, 15),
				new Token(Token.TOKEN_TYPE_NEWLINE, "\n", 16, 17, 2, 15)))
        );
    }
	
	private static <U> List<U> iteratorToList(Iterator<U> iterator) {
		List<U> list = new ArrayList<>();
		while (iterator.hasNext()) {
			U next = iterator.next();
			if (next == null) {
				break;
			}
			list.add(next);
		}
		return list;
	}
}