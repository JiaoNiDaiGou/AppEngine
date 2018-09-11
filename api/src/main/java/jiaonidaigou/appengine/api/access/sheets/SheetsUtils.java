package jiaonidaigou.appengine.api.access.sheets;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.Request;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class SheetsUtils {
    public static String rowRange(final int sheetId, final int rowStart, final int rowEnd) {
        return String.format("%d!%d:%d", sheetId, rowStart, rowEnd);
    }

    public static String colRange(final int sheetId, final int colStart, final int colEnd) {
        return String.format("%d!%s:%s", sheetId, col(colStart), col(colEnd));
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
}
