package jiaonidaigou.appengine.api.utils;

import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;

import static jiaonidaigou.appengine.common.utils.Environments.GCS_MEDIA_ROOT_ENDSLASH;

public class MediaUtils {
    public static String determineMediaType(final String pathOrFileExtension) {
        String ext = pathOrFileExtension.contains(".")
                ? StringUtils.substringAfterLast(pathOrFileExtension, ".").toLowerCase()
                : pathOrFileExtension.toLowerCase();
        switch (ext) {
            case "txt":
                return MediaType.PLAIN_TEXT_UTF_8.toString();
            case "jpg":
                return MediaType.JPEG.toString();
            case "png":
                return MediaType.PNG.toString();
            case "gif":
                return MediaType.GIF.toString();
            case "pdf":
                return MediaType.PDF.toString();
            default:
                return MediaType.OCTET_STREAM.toString();
        }
    }

    public static String toStoragePath(final String mediaId) {
        return GCS_MEDIA_ROOT_ENDSLASH + mediaId;
    }
}
