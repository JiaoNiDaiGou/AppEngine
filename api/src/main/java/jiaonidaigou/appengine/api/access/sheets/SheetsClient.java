package jiaonidaigou.appengine.api.access.sheets;

import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface SheetsClient {
    Spreadsheet getSheet(final String sheetId);

    Sheet getTab(final SheetTab tab);

    String createSheet(final String name);

    /**
     * Gets cell values by range (in a1notation format).
     */
    List<List<String>> getCells(final String sheetId, final String a1notationRange);

    void putCells(final String sheetId, final String a1notationRange, List<List<String>> cells);

    /**
     * Append cells to the end of the given tab.
     */
    void appendRows(final SheetTab sheetTab, final List<List<String>> cells);

    /**
     * Append cells to the end of the given tab.
     */
    default void appendRow(final SheetTab sheetTab, final List<String> cells) {
        appendRows(sheetTab, Collections.singletonList(cells));
    }

    /**
     * Add new tab with given title.
     */
    SheetTab addTab(final String sheetId, final String title);

    /**
     * Delete all content on given tab.
     */
    void clearTab(final SheetTab tab);

    /**
     * Delete given tab.
     */
    void deleteTab(final SheetTab tab);

    /**
     * Delete rows.
     */
    void deleteRows(final SheetTab sheetTab, final int rowStart, final int rowEnd);

    /**
     * Delete columns
     */
    void deleteCols(final SheetTab sheetTab, final int colStart, final int colEnd);

    /**
     * Gets all tab's id and name.
     */
    Map<SheetTab, String> allTabs(final String sheetId);
}
