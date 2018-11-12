package jiaoni.common.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtils2Test {
    @Test
    public void testRemoveNonLettersNumbersOrChineseLetters() {
        assertEquals("abc", StringUtils2.removeNonCharTypesWith("a b c", StringUtils2.CharType.A2Z, StringUtils2.CharType.CHINESE));
        assertEquals("abc", StringUtils2.removeNonCharTypesWith("a\t b -% c", StringUtils2.CharType.A2Z, StringUtils2.CharType.CHINESE));
        assertEquals("中国", StringUtils2.removeNonCharTypesWith("中、国", StringUtils2.CharType.A2Z, StringUtils2.CharType.CHINESE));
    }

    @Test
    public void testRemoveEveryMatch() {
        assertEquals(
                ImmutableList.of("ab1c1d", "a1bc1d", "a1b1cd"),
                StringUtils2.removeEveryMatch("a1b1c1d", "1"));

        assertEquals(
                ImmutableList.of("a1b1c1d1", "1ab1c1d1", "1a1bc1d1", "1a1b1cd1", "1a1b1c1d"),
                StringUtils2.removeEveryMatch("1a1b1c1d1", "1"));
    }

    @Test
    public void testReplaceLast() {
        String str = "aaa_bbb_bbb_bbb_ccc";
        String find = "bbb";
        String result = StringUtils2.replaceLast(str, find, "zzz");
        assertEquals("aaa_bbb_bbb_zzz_ccc", result);
    }
}
