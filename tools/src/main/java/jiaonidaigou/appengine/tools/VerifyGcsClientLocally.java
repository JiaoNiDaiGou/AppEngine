package jiaonidaigou.appengine.tools;

import com.google.api.services.sheets.v4.Sheets;
import jiaonidaigou.appengine.api.access.storage.GcsClient;
import jiaonidaigou.appengine.api.utils.GApisFactory;

public class VerifyGcsClientLocally {
    public static void main(String[] args) {
        GcsClient gcsClient = new GcsClient(GApisFactory.storage());
//        GcsClient gcsClient = new GcsClient(GApisFactory.storage());
//
//        String path = Consts.GCS_MEDIA_ROOT_ENDSLASH + "51ba9647-5280-4066-8226-9b3c85aaa9d0";
//
//        byte[] bytes = gcsClient.read(path);
//        Sheets sheets = GApisFactory.sheets();
    }
}
