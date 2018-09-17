package jiaonidaigou.appengine.common.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static jiaonidaigou.appengine.common.utils.StringUtils2.CharType.A2Z;
import static jiaonidaigou.appengine.common.utils.StringUtils2.CharType.CHINESE;
import static jiaonidaigou.appengine.common.utils.StringUtils2.removeEveryMatch;
import static jiaonidaigou.appengine.common.utils.StringUtils2.removeNonCharTypesWith;
import static org.junit.Assert.assertEquals;

public class StringUtils2Test {
    @Test
    public void testRemoveNonLettersNumbersOrChineseLetters() {
        assertEquals("abc", removeNonCharTypesWith("a b c", A2Z, CHINESE));
        assertEquals("abc", removeNonCharTypesWith("a\t b -% c", A2Z, CHINESE));
        assertEquals("中国", removeNonCharTypesWith("中、国", A2Z, CHINESE));
    }

    @Test
    public void testRemoveEveryMatch() {
        assertEquals(
                ImmutableList.of("ab1c1d", "a1bc1d", "a1b1cd"),
                removeEveryMatch("a1b1c1d", "1"));

        assertEquals(
                ImmutableList.of("a1b1c1d1", "1ab1c1d1", "1a1bc1d1", "1a1b1cd1", "1a1b1c1d"),
                removeEveryMatch("1a1b1c1d1", "1"));
    }
}
