package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.access.sheets.GoogleSheetsClient;
import jiaonidaigou.appengine.api.access.sheets.SheetsClient;
import jiaonidaigou.appengine.api.utils.GApisFactory;

public class VerifyGoogleSheetsLocally {
    public static void main(String[] args) {
        SheetsClient client = new GoogleSheetsClient(GApisFactory.sheets());

        String sheetId = client.createSheet("sheet_title");
    }
}
