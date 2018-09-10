package jiaonidaigou.appengine.api.access.sheets;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Convert col/row number to A1 notation.
 * col and row number is 1 based.
 */
public class A1Notations {
    public static String rowRange(final String tabId, final int rowStart, final int rowEnd) {
        return String.format("%s!%d:%d", tabId, rowStart, rowEnd);
    }

    public static String colRange(final String tabId, final int colStart, final int colEnd) {
        return String.format("%s!%s:%s", tabId, col(colStart), col(colEnd));
    }

    public static String col(final int colNum) {
        checkArgument(colNum > 0);
        StringBuilder toReturn = new StringBuilder();
        int temp = colNum;
        do {
            int k = temp % 26;
            char right = k == 0 ? 'Z' : (char) ('A' + (k - 1));
            toReturn.insert(0, right);
            temp /= 26;
        }
        while (temp != 0);
        return toReturn.toString();
    }
}
