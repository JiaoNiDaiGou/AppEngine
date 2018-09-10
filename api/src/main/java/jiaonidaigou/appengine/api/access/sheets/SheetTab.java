package jiaonidaigou.appengine.api.access.sheets;

import com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;

import static jiaonidaigou.appengine.common.utils.Preconditions2.checkNotBlank;

public class SheetTab {
    private final String sheetId;
    private final String tabId;

    SheetTab(final String sheetId, final String tabId) {
        this.sheetId = checkNotBlank(sheetId);
        this.tabId = checkNotBlank(tabId);
    }

    /**
     * Creates SheetTab based Google sheets URL.
     * The URL looks like
     * https://docs.google.com/spreadsheets/d/1a1b9etDGxhyLYqhYTMd5D3Z7RvKjTIjOy0td4AiaID0/edit#gid=771367354
     * The sheetId (identifier of the sheet) is 1a1b9etDGxhyLYqhYTMd5D3Z7RvKjTIjOy0td4AiaID0
     * The tabId (the tab inside sheet) is 771367354
     */
    public static SheetTab ofGoogleSheet(final String path) {
        checkNotBlank(path);
        String sheetId = StringUtils.substringBetween(path, "https://docs.google.com/spreadsheets/d/", "/");
        String tabId = StringUtils.substringAfterLast(path, "gid=");
        return new SheetTab(sheetId, tabId);
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getTabId() {
        return tabId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SheetTab sheetTab = (SheetTab) o;
        return Objects.equal(sheetId, sheetTab.sheetId) &&
                Objects.equal(tabId, sheetTab.tabId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sheetId, tabId);
    }
}
