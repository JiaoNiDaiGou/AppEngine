package jiaoni.common.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class StringUtils2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringUtils2.class);

    /**
     * Given a list of splits flag, split the string with any flag in it.
     * E.g. multiSplit("111#222$333", "#", "$") will return ["111", "222", "333"]
     */
    public static String[] multiSplit(String str, String... splits) {
        if (str == null) {
            return null;
        }

        List<String> cur = new ArrayList<>();
        cur.add(str);
        for (String split : splits) {
            List<String> next = new ArrayList<>();
            for (String text : cur) {
                String[] parts = StringUtils.split(text, split);
                next.addAll(Arrays.asList(parts));
            }
            cur = next;
        }
        return cur.toArray(new String[]{ });
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

    /**
     * Return number of lines in given string.
     */
    public static int linesOfString(final String str) {
        if (StringUtils.isBlank(str)) {
            return 0;
        }
        return StringUtils.countMatches(str, "\n");
    }

    /**
     * Align text to center.
     */
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

    /**
     * Align text to right.
     */
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

    /**
     * Generate a list of string. Each element is the output of removing given match from input string once.
     * E.g.
     * removeEveryMatch("1a2a3a4a", "a") => [ "12a3a4a", "1a23a4a", "1a2a34a", "1a2a3a4" ]
     */
    public static List<String> removeEveryMatch(final String str, final String match) {
        return replaceEveryMatch(str, match, "");
    }

    /**
     * Generate a list of string. Each element is the output of removing given match from input string once.
     * E.g.
     * removeEveryMatch("1a2a3a4a", "a") => [ "12a3a4a", "1a23a4a", "1a2a34a", "1a2a3a4" ]
     */
    public static List<String> replaceEveryMatch(final String str, final String match, final String replace) {
        if (StringUtils.isBlank(str)) {
            return ImmutableList.of();
        }
        if (StringUtils.isBlank(match) || !str.contains(match)) {
            return ImmutableList.of(str);
        }

        List<String> toReturn = new ArrayList<>();
        int start = 0;
        while (start < str.length()) {
            start = StringUtils.indexOf(str, match, start);
            if (start == -1) {
                break;
            }

            toReturn.add(str.substring(0, start)
                    + replace
                    + str.substring(start + match.length(), str.length()));
            start += match.length();
        }
        return toReturn;
    }

    /**
     * Check given char matches in given type.
     */
    public static boolean isCharType(final char ch, final CharType type) {
        checkNotNull(type);
        switch (type) {
            case DIGIT:
                return ch >= '0' && ch <= '9';
            case A2Z:
                return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
            case CHINESE:
                return ch >= 0x4E00 && ch <= 0x9FA5;
            case SPACE:
                return ch == ' ';
            default:
                throw new IllegalStateException("unexpected char type " + type);
        }
    }

    /**
     * Check given char matches any of given types.
     */
    public static boolean isAnyCharType(final char ch, final CharType... types) {
        checkArgument(types.length > 0);
        for (CharType type : types) {
            if (isCharType(ch, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * If string only contains given types of chars.
     */
    public static boolean containsOnlyCharTypes(final String str, final CharType... types) {
        checkArgument(types.length > 0);
        if (str == null) {
            return false;
        }
        for (char ch : str.toCharArray()) {
            if (!isAnyCharType(ch, types)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove chars which are not in the given types.
     */
    public static String removeNonCharTypesWith(final String str,
                                                final CharType... charTypesToKeep) {
        return replaceNonCharTypesWith(str, charTypesToKeep, "", null);
    }

    /**
     * Remove chars which are not in the given types.
     */
    public static String removeNonCharTypesWith(final String str,
                                                final CharType[] charTypesToKeep,
                                                @Nullable final char[] excepts) {
        return replaceNonCharTypesWith(str, charTypesToKeep, "", excepts);
    }

    /**
     * Replace chars which are not in the given types.
     */
    public static String replaceNonCharTypesWith(final String str,
                                                 final CharType[] charTypesToKeep,
                                                 @Nullable final String replace) {
        return replaceNonCharTypesWith(str, charTypesToKeep, replace, null);
    }

    /**
     * Replace chars which are not in given types.
     */
    public static String replaceNonCharTypesWith(final String str,
                                                 final CharType[] charTypesToKeep,
                                                 @Nullable final String replace,
                                                 @Nullable final char[] excepts) {
        checkArgument(charTypesToKeep.length > 0);
        if (str == null) {
            return null;
        }

        Set<Character> exceptChars = ImmutableSet.of();
        if (excepts != null) {
            exceptChars = new HashSet<>();
            for (char ch : excepts) {
                exceptChars.add(ch);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray()) {
            if (isAnyCharType(ch, charTypesToKeep) || exceptChars.contains(ch)) {
                sb.append(ch);
            } else {
                sb.append(replace);
            }
        }
        return sb.toString();
    }

    /**
     * Replace chars which are not in given types.
     */
    public static String replaceNonCharTypesWith(final String str,
                                                 final CharType[] charTypesToKeep,
                                                 @Nullable final char replace,
                                                 @Nullable final char[] excepts) {
        return replaceNonCharTypesWith(str, charTypesToKeep, String.valueOf(replace), excepts);
    }

    public static String removeDuplicatedSpaces(final String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        return String.join(" ", StringUtils.split(str.trim(), " "));
    }

    public static String replaceLast(String str, String find, String repl) {
        int idx = StringUtils.lastIndexOf(str, find);
        if (idx < 0) {
            return str;
        }
        return str.substring(0, idx) + repl + str.substring(idx + find.length(), str.length());
    }

    public enum CharType {
        DIGIT, // 0 - 9
        A2Z, // a-z && A-Z
        CHINESE,
        SPACE,
    }
}
