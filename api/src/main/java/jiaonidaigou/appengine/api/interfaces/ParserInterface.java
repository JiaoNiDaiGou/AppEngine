package jiaonidaigou.appengine.api.interfaces;


import com.google.common.io.ByteStreams;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.common.model.InternalIOException;
import jiaonidaigou.appengine.lib.ocrspace.OcrSpaceClient;
import jiaonidaigou.appengine.lib.ocrspace.model.ParseRequest;
import jiaonidaigou.appengine.lib.ocrspace.model.ParseResponse;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.api.Consts.GCS_MEDIA_ROOT_ENDSLASH;

@Path("/api/parser")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class ParserInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserInterface.class);

    private StorageClient storageClient;
    private OcrSpaceClient ocrSpaceClient;

    @Inject
    public ParserInterface(final StorageClient storageClient,
                           final OcrSpaceClient ocrSpaceClient) {
        this.storageClient = storageClient;
        this.ocrSpaceClient = ocrSpaceClient;
    }

    @GET
    @Path("/customer/image")
    public Response parseCustomerByImage(@QueryParam("media_id") final String mediaId) {
        RequestValidator.validateNotBlank(mediaId, "media_id");

        String path = GCS_MEDIA_ROOT_ENDSLASH + mediaId;
        if (!storageClient.exists(path)) {
            throw new NotFoundException("no media found by id " + mediaId);
        }

//        // TODO:
//        // Use signed download url
//        byte[] media;
//        try (InputStream inputStream = storageClient.inputStream(path)) {
//            media = ByteStreams.toByteArray(inputStream);
//        } catch (IOException e) {
//            throw new InternalIOException("failed to read from " + path, e);
//        }
//
//        ParseResponse parseResponse = ocrSpaceClient.parse(ParseRequest.builder()
//                .withFileType(ParseRequest.FileType.JPG)
//                .withImageBytes(media)
//                .build());
//
//        return Response.ok(parseResponse.getAllParsedText()).build();
        return null;
    }
}
