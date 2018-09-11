package jiaonidaigou.appengine.api.interfaces;

import com.google.common.base.Charsets;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaonidaigou.appengine.api.Consts.GCS_MEDIA_ROOT_ENDSLASH;

@Path("/api/media")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class MediaInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaInterface.class);

    private final StorageClient storageClient;

    @Inject
    public MediaInterface(final StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @GET
    @Path("/write")
    public Response write(@QueryParam("input") final String input) {
        String path = GCS_MEDIA_ROOT_ENDSLASH + "temp.txt";
        storageClient.write(path, MediaType.TEXT_PLAIN.toString(), "tehis is write".getBytes(Charsets.UTF_8));
        return Response.ok(path).build();
    }

    @GET
    @Path("/url/upload")
    public Response getUploadSignedUrl(@QueryParam("ext") final String fileExtension) {
        RequestValidator.validateNotBlank(fileExtension, "fileExtension");

        String mediaId = UUID.randomUUID().toString() + "." + fileExtension.toLowerCase();
        String mediaType = determineMediaType(fileExtension);
        String path = GCS_MEDIA_ROOT_ENDSLASH + mediaId;
        LOGGER.info("upload mediaId={}, path={}, mediaType={}", mediaId, path, mediaType);

        String signedUrl = storageClient.getSignedUploadUrl(path, mediaType);
        return Response.ok(MediaObject.newBuilder()
                .setId(mediaId)
                .setFullPath(path)
                .setSignedUploadUrl(signedUrl)
                .build())
                .build();
    }

    @GET
    @Path("/url/download")
    public Response getSignedDownloadUrl(@QueryParam("media_id") final String mediaId) {
        RequestValidator.validateNotBlank(mediaId, "media_id");

        String path = GCS_MEDIA_ROOT_ENDSLASH + mediaId;
        String mediaType = determineMediaType(path);
        LOGGER.info("upload mediaId={}, path={}, mediaType={}", mediaId, path, mediaType);

        String signedUrl = storageClient.getSignedDownloadUrl(path, mediaType);
        return Response.ok(MediaObject.newBuilder()
                .setId(mediaId)
                .setFullPath(path)
                .setSignedDownloadUrl(signedUrl)
                .build())
                .build();
    }

    private static String determineMediaType(final String pathOrFileExtension) {
        String ext = pathOrFileExtension.contains(".")
                ? StringUtils.substringAfterLast(pathOrFileExtension, ".").toLowerCase()
                : pathOrFileExtension.toLowerCase();
        switch (ext) {
            case "txt":
                return MediaType.TEXT_PLAIN_TYPE.toString();
            default:
                return MediaType.APPLICATION_OCTET_STREAM_TYPE.toString();
        }
    }
}
