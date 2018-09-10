package jiaonidaigou.appengine.api.access.sheets;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.AddSheetResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import jiaonidaigou.appengine.common.model.InternalIOException;
import jiaonidaigou.appengine.common.model.InternalRuntimeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class GoogleSheetsClient implements SheetsClient {
    private static final String ROW_DIMENSION = "ROWS";
    private static final String COL_DIMENSION = "COLUMNS";
    private static final String DEFAULT_VALUE_INPUT_OPTION = "USER_ENTERED";

    private final Sheets client;

    @Inject
    public GoogleSheetsClient(final Sheets client) {
        this.client = client;
    }

    private static List<List<Object>> convertToObjectMatrix(final List<List<String>> stringData) {
        if (stringData == null) {
            return null;
        }
        List<List<Object>> objectData = new ArrayList<>();
        for (List<String> stringRow : stringData) {
            objectData.add(new ArrayList<>(stringRow));
        }
        return objectData;
    }

    private static List<List<String>> convertToStringMatrix(final List<List<Object>> objectData) {
        if (objectData == null) {
            return null;
        }
        List<List<String>> stringData = new ArrayList<>();
        for (List<Object> objectRow : objectData) {
            List<String> stringRow = new ArrayList<>();
            for (Object elem : objectRow) {
                stringRow.add((String) elem);
            }
            stringData.add(stringRow);
        }
        return stringData;
    }

    /*
     * Forms the BatchUpdate POJO for deleting specific rows or columns
     *
     * https://developers.google.com/sheets/samples/rowcolumn#delete_rows_or_columns
     */
    private Request updateRequest(final String tabId,
                                  final String dimension,
                                  final int startIndex,
                                  final int endIndex) {
        DimensionRange range = new DimensionRange()
                .setSheetId(Integer.parseInt(tabId))
                .setDimension(dimension)
                .setStartIndex(startIndex)
                .setEndIndex(endIndex);
        DeleteDimensionRequest request = new DeleteDimensionRequest()
                .setRange(range);
        return new Request().setDeleteDimension(request);
    }

    private BatchUpdateSpreadsheetResponse batchUpdate(final String sheetId,
                                                       final Request... requests) {
        try {
            BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Arrays.asList(requests));
            return client.spreadsheets()
                    .batchUpdate(sheetId, batchRequest)
                    .execute();
        } catch (IOException e) {
            throw new InternalRuntimeException(e);
        }
    }

    @Override
    public List<List<String>> getCells(final String sheetId, final String range) {
        ValueRange response;
        try {
            response = client.spreadsheets()
                    .values()
                    .get(sheetId, range)
                    .execute();
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        return convertToStringMatrix(response.getValues());
    }

    @Override
    public void putCells(final String sheetId, final String range, final List<List<String>> cells) {
        // See https://developers.google.com/sheets/reference/rest/v4/spreadsheets.values/update
        ValueRange valueRange = new ValueRange()
                .setMajorDimension(ROW_DIMENSION)
                .setRange(range)
                .setValues(convertToObjectMatrix(cells));
        try {
            client.spreadsheets()
                    .values()
                    .update(sheetId, range, valueRange)
                    .setValueInputOption(DEFAULT_VALUE_INPUT_OPTION)
                    .execute();
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public void appendRows(final SheetTab sheetTab, final List<List<String>> cells) {
        Sheet sheet = getTab(sheetTab);
        if (sheet == null) {
            return;
        }

        int maxRow = sheet.getProperties().getGridProperties().getRowCount();
        String range = A1Notations.rowRange(sheetTab.getTabId(), maxRow + 1, maxRow + cells.size());
        putCells(sheetTab.getSheetId(), range, cells);
    }

    @Override
    public SheetTab addTab(final String sheetId, String title) {
        // See https://developers.google.com/sheets/samples/sheet#add_a_sheet

        AddSheetRequest addSheetRequest = new AddSheetRequest()
                .setProperties(new SheetProperties()
                        .setTitle(title));
        BatchUpdateSpreadsheetResponse response = batchUpdate(sheetId, new Request().setAddSheet(addSheetRequest));

        // should only contain one response
        AddSheetResponse addSheetResponse = response.getReplies().get(0).getAddSheet();

        return new SheetTab(sheetId, addSheetResponse.getProperties().getSheetId().toString());
    }

    @Override
    public void clearTab(final SheetTab tab) {
        // See https://developers.google.com/sheets/samples/sheet#clear_a_sheet_of_all_values_while_preserving_formats
        UpdateCellsRequest request = new UpdateCellsRequest()
                .setRange(new GridRange().setSheetId(Integer.parseInt(tab.getTabId())))
                .setFields("*");
        batchUpdate(tab.getSheetId(), new Request().setUpdateCells(request));
    }

    @Override
    public void deleteTab(final SheetTab tab) {
        DeleteSheetRequest request = new DeleteSheetRequest().setSheetId(Integer.parseInt(tab.getTabId()));
        batchUpdate(tab.getSheetId(), new Request().setDeleteSheet(request));
    }

    @Override
    public void deleteRows(final SheetTab sheetTab, final int rowStart, final int rowEnd) {
        batchUpdate(sheetTab.getSheetId(), updateRequest(sheetTab.getTabId(), ROW_DIMENSION, rowStart, rowEnd));
    }

    @Override
    public void deleteCols(final SheetTab sheetTab, final int colStart, final int colEnd) {
        batchUpdate(sheetTab.getSheetId(), updateRequest(sheetTab.getTabId(), COL_DIMENSION, colStart, colEnd));
    }

    @Override
    public Map<SheetTab, String> allTabs(final String sheetId) {
        Spreadsheet spreadsheet = getSheet(sheetId);
        Map<SheetTab, String> toReturn = new HashMap<>();
        for (Sheet sheet : spreadsheet.getSheets()) {
            String tabId = sheet.getProperties().getSheetId().toString();
            String title = sheet.getProperties().getTitle();
            toReturn.put(new SheetTab(sheetId, tabId), title);
        }
        return toReturn;
    }

    @Override
    public Spreadsheet getSheet(final String sheetId) {
        try {
            return client.spreadsheets()
                    .get(sheetId)
                    .execute();
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    @Override
    public Sheet getTab(final SheetTab tab) {
        return getSheet(tab.getSheetId())
                .getSheets()
                .stream()
                .filter(t -> tab.getTabId().equals(t.getProperties().getSheetId().toString()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String createSheet(final String name) {
        Spreadsheet sheet;
        try {
            sheet = client.spreadsheets()
                    .create(new Spreadsheet().setProperties(
                            new SpreadsheetProperties().setTitle(name)
                    ))
                    .execute();
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        return sheet.getSpreadsheetId();
    }
}

