package jiaoni.common.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import static jiaoni.common.utils.StringUtils2.removeNonCharTypesWith;
import static org.junit.Assert.assertEquals;

public class StringUtils2Test {
    @Test
    public void testRemoveNonLettersNumbersOrChineseLetters() {
        Assert.assertEquals("abc", StringUtils2.removeNonCharTypesWith("a b c", StringUtils2.CharType.A2Z, StringUtils2.CharType.CHINESE));
        Assert.assertEquals("abc", StringUtils2.removeNonCharTypesWith("a\t b -% c", StringUtils2.CharType.A2Z, StringUtils2.CharType.CHINESE));
        Assert.assertEquals("中国", StringUtils2.removeNonCharTypesWith("中、国", StringUtils2.CharType.A2Z, StringUtils2.CharType.CHINESE));
    }

    @Test
    public void testRemoveEveryMatch() {
        Assert.assertEquals(
                ImmutableList.of("ab1c1d", "a1bc1d", "a1b1cd"),
                StringUtils2.removeEveryMatch("a1b1c1d", "1"));

        Assert.assertEquals(
                ImmutableList.of("a1b1c1d1", "1ab1c1d1", "1a1bc1d1", "1a1b1cd1", "1a1b1c1d"),
                StringUtils2.removeEveryMatch("1a1b1c1d1", "1"));
    }
}
