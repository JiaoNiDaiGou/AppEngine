package jiaonidaigou.appengine.api.access.sheets;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class SheetsUtils {
    // Path looks like
    // https://docs.google.com/spreadsheets/d/1a1b9etDGxhyLYqhYTMd5D3Z7RvKjTIjOy0td4AiaID0/edit#gid=771367354
    // Spreadsheet id is 1a1b9etDGxhyLYqhYTMd5D3Z7RvKjTIjOy0td4AiaID0
    public static String extractSpreadsheetId(final String path) {
        checkArgument(StringUtils.isNotBlank(path));
        return StringUtils.substringBetween(path, "https://docs.google.com/spreadsheets/d/", "/");
    }

    // Path looks like
    // https://docs.google.com/spreadsheets/d/1a1b9etDGxhyLYqhYTMd5D3Z7RvKjTIjOy0td4AiaID0/edit#gid=771367354
    // Sheet id is 771367354
    public static int extractSheetId(final String path) {
        checkArgument(StringUtils.isNotBlank(path));
        String str = StringUtils.substringAfterLast(path, "gid=");
        return Integer.parseInt(str);
    }

    public static String rowRange(final Sheet sheet, final int rowNum) {
        return rowRange(sheet, rowNum, rowNum);
    }

    public static String rowRange(final Sheet sheet, final int rowStart, final int rowEnd) {
        return a1Notation(sheet.getProperties().getTitle(), String.valueOf(rowStart), String.valueOf(rowEnd));
    }

    public static String a1NotationColRange(final Sheet sheet, final int colStart, final int colEnd) {
        return a1Notation(sheet.getProperties().getTitle(), col(colStart), col(colEnd));
    }

    public static String a1NotationColRange(final Sheet sheet, final int colNum) {
        return a1NotationColRange(sheet, colNum, colNum);
    }

    private static String a1Notation(final String sheetTitle, final String from, final String to) {
        return String.format("%s!%s:%s", sheetTitle, from, to);
    }

    private static String col(final int colNum) {
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

    public static BatchUpdateSpreadsheetResponse batchUpdate(
            final Sheets sheets,
            final String spreadsheetId,
            final Request request)
            throws IOException {
        return batchUpdate(sheets, spreadsheetId, Collections.singletonList(request));
    }

    public static BatchUpdateSpreadsheetResponse batchUpdate(
            final Sheets sheets,
            final String spreadsheetId,
            final List<Request> requests)
            throws IOException {
        return sheets.spreadsheets().batchUpdate(
                spreadsheetId,
                new BatchUpdateSpreadsheetRequest().setRequests(requests))
                .execute();
    }

    public static List<List<String>> toStringMatrix(final ValueRange valueRange) {
        if (valueRange == null || valueRange.getValues() == null) {
            return null;
        }
        List<List<String>> stringData = new ArrayList<>();
        for (List<Object> objectRow : valueRange.getValues()) {
            List<String> stringRow = new ArrayList<>();
            for (Object elem : objectRow) {
                stringRow.add((String) elem);
            }
            stringData.add(stringRow);
        }
        return stringData;
    }
}
