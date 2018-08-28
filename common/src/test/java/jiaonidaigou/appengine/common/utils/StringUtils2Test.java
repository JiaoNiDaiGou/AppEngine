package jiaonidaigou.appengine.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtils2Test {
    @Test
    public void testRemoveNonLettersNumbersOrChineseLetters() {
        assertEquals("abc", StringUtils2.removeNonLettersNumbersOrChineseLetters("a b c"));
        assertEquals("abc", StringUtils2.removeNonLettersNumbersOrChineseLetters("a\t b -% c"));
        assertEquals("中国", StringUtils2.removeNonLettersNumbersOrChineseLetters("中、国"));
    }
}
