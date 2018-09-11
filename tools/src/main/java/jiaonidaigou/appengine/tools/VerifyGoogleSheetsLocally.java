package jiaonidaigou.appengine.tools;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import jiaonidaigou.appengine.api.access.gcp.GoogleApisClientFactory;
import jiaonidaigou.appengine.api.access.sheets.SheetsUtils;

public class VerifyGoogleSheetsLocally {
    public static void main(String[] args) throws Exception {
        // Init client.
        Sheets sheets = GoogleApisClientFactory.sheets();

        // Create a sheet.
        String title = "this is a spreadsheet title";
        Spreadsheet spreadsheet = sheets
                .spreadsheets()
                .create(new Spreadsheet()
                        .setProperties(new SpreadsheetProperties().setTitle(title)))
                .execute();
        System.out.println("create a spreadsheet " + spreadsheet.getSpreadsheetId());
        System.out.println(spreadsheet.getSpreadsheetUrl());
        String spreadsheetId = spreadsheet.getSpreadsheetId();

        // Create a tab
        Request addSheetRequest = new Request()
                .setAddSheet(new AddSheetRequest()
                        .setProperties(new SheetProperties().setTitle("this is a tab title")));
        int sheetId = SheetsUtils.batchUpdate(sheets, spreadsheetId, addSheetRequest)
                .getReplies()
                .get(0)
                .getAddSheet()
                .getProperties()
                .getSheetId();
        System.out.println("create a sheet " + sheetId);
    }
}
