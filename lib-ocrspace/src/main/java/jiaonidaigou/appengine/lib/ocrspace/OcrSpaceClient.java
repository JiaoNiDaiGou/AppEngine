package jiaonidaigou.appengine.lib.ocrspace;

import com.google.common.base.Charsets;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.common.model.InternalIOException;
import jiaonidaigou.appengine.common.utils.Secrets;
import jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest;
import jiaonidaigou.appengine.lib.ocrspace.model.ParseResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkNotNull;

public class OcrSpaceClient {
    private static final String ENDPOINT_IMAGEURL = "https://api.ocr.space/parse/image";

    private final String apiKey;
    private final MockBrowserClient client;

    public OcrSpaceClient(final MockBrowserClient client) {
        this.client = client;

        //
        // Limit:
        //   500/day
        //   25,000/month
        //   1mb per file
        this.apiKey = Secrets.of("ocrspace.apikey").getAsString();
    }

    private static String toBase64Image(final File file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        bytes = Base64.getEncoder().encode(bytes);
        return "data:image/jpg;base64," + new String(bytes, Charsets.UTF_8);
    }

    public ParseResponse parse(final ParseRequest request) {
        checkNotNull(request);

        MultipartEntityBuilder builder = MultipartEntityBuilder
                .create()
                .addTextBody("apikey", apiKey)
                .addTextBody("language", request.getLanguage().getValue())
                .addTextBody("isOverlayRequired", String.valueOf(request.isOverlayRequired()))
                .addTextBody("isCreateSearchablePdf", String.valueOf(request.isCreateSearchablePdf()))
                .addTextBody("filetype", request.getFileType().name());
        if (StringUtils.isNotBlank(request.getImageUrl())) {
            builder.addTextBody("url", request.getImageUrl());
        } else if (request.getImageFile() != null) {
            builder.addPart("file", new FileBody(request.getImageFile()));

            // Alternatively,
            // builder.addTextBody("base64Image", encodeFileToBase64Binary(request.getImageFile()));

        } else if (request.getImageBytes() != null) {
            builder.addPart("file", new ByteArrayBody(request.getImageBytes(), "random." + request.getFileType()));
        } else {
            builder.addPart("file", new InputStreamBody(request.getImage(), "random." + request.getFileType()));
        }

        return client.doPost()
                .url(ENDPOINT_IMAGEURL)
                .body(builder.build())
                .request()
                .callToJson(ParseResponse.class);
    }
}
