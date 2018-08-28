package jiaonidaigou.appengine.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class StringUtils2 {
    public static String removeNonLettersNumbersOrChineseLetters(final String input, char... includes) {
        return replaceNonLettersNumbersOrChineseLetters(input, null, includes);
    }

    public static boolean containsOnlyChineseLetters(final String input) {
        if (input == null) {
            return false;
        }
        for (char ch : input.toCharArray()) {
            if (!(ch >= 0x4E00 && ch <= 0x9FA5)) {
                return false;
            }
        }
        return true;
    }

    public static String replaceNonLettersNumbersOrChineseLetters(final String input, Character toReplace, char... includes) {
        if (input == null) {
            return null;
        }
        Set<Character> includeSet = new HashSet<>();
        for (char ch : includes) {
            includeSet.add(ch);
        }

        StringBuilder builder = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if ((ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= 0x4E00 && ch <= 0x9FA5)
                    || includeSet.contains(ch)) {
                builder.append(ch);
            } else if (toReplace != null) {
                builder.append(toReplace);
            }
        }
        return builder.toString();
    }

    public static String concatStringExceptIndexAt(final String[] str, final String splitter, final int... exceptIndex) {
        Set<Integer> exclude = new HashSet<>();
        Arrays.stream(exceptIndex).forEach(exclude::add);
        return IntStream.range(0, str.length)
                .filter(i -> !exclude.contains(i))
                .mapToObj(i -> str[i])
                .reduce((a, b) -> a + splitter + b)
                .orElse(null);
    }

    public static int linesOfString(final String str) {
        if (StringUtils.isBlank(str)) {
            return 0;
        }
        return StringUtils.countMatches(str, "\n");
    }

    public static String textAlignCenter(final String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        List<String> lines = Arrays.asList(StringUtils.split(text, "\n"));
        int maxWidth = lines.stream().map(String::length).max(Integer::compareTo).orElse(0);
        return lines.stream()
                .map(t -> {
                    int width = t.length();
                    int padTotal = maxWidth - width;
                    int padLeft = padTotal / 2;
                    int padRight = padTotal - padLeft;
                    String toReturn = t;
                    if (padLeft > 0) {
                        toReturn = StringUtils.leftPad(toReturn, padLeft, ' ');
                    }
                    if (padRight > 0) {
                        toReturn = StringUtils.rightPad(toReturn, padRight, ' ');
                    }
                    return toReturn;
                })
                .reduce((a, b) -> a + "\n" + b)
                .orElse(null);
    }

    public static String textAlignRight(final String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        List<String> lines = Arrays.asList(StringUtils.split(text, "\n"));
        int maxWidth = lines.stream().map(String::length).max(Integer::compareTo).orElse(0);
        return lines.stream()
                .map(t -> {
                    int padLeft = maxWidth - t.length();
                    if (padLeft == 0) {
                        return t;
                    }
                    return StringUtils.leftPad(t, padLeft, ' ');
                })
                .reduce((a, b) -> a + "\n" + b)
                .orElse(null);
    }
}
