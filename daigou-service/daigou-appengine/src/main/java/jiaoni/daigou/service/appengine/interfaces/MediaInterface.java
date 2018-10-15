package jiaoni.daigou.service.appengine.interfaces;

import com.google.common.io.ByteStreams;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.MediaUtils;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.model.InternalIOException;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.common.wiremodel.MediaObject;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaoni.common.appengine.utils.MediaUtils.determineMediaType;

@Path("/api/media")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class MediaInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaInterface.class);

    private final StorageClient storageClient;

    @Inject
    public MediaInterface(final StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @GET
    @Path("/url/upload")
    public Response getUploadSignedUrl(@QueryParam("ext") final String fileExtension) {
        RequestValidator.validateNotBlank(fileExtension, "fileExtension");

        String mediaId = UUID.randomUUID().toString() + "." + fileExtension.toLowerCase();
        String mediaType = determineMediaType(fileExtension);
        String path = MediaUtils.toGcsPath(mediaId);
        LOGGER.info("upload mediaId={}, path={}, mediaType={}", mediaId, path, mediaType);

        URL signedUrl = storageClient.getSignedUploadUrl(path, mediaType);
        return Response.ok(MediaObject.newBuilder()
                .setId(mediaId)
                .setFullPath(path)
                .setSignedUploadUrl(signedUrl.toString())
                .setMediaType(mediaType)
                .build())
                .build();
    }

    @POST
    @Path("/formDirectUpload")
    @Consumes({ MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON })
    public Response directUpload(@QueryParam("ext") final String fileExtension,
                                 @QueryParam("hasDownloadUrl") final boolean hasDownloadUrl,
                                 @FormDataParam("file") InputStream fileInputStream) {
        RequestValidator.validateNotBlank(fileExtension, "fileExtension");
        byte[] body;
        try (InputStream inputStream = fileInputStream) {
            body = ByteStreams.toByteArray(inputStream);
            LOGGER.info("ready bytes length: " + body.length);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }

        String mediaId = UUID.randomUUID().toString() + "." + fileExtension.toLowerCase();
        String mediaType = determineMediaType(fileExtension);
        String path = MediaUtils.toGcsPath(mediaId);
        LOGGER.info("directUpload mediaId={}, path={}, mediaType={}, bytes.length={}", mediaId, path, mediaType, body.length);

        storageClient.write(path, mediaType, body);

        URL signedUrl = null;
        if (hasDownloadUrl) {
            signedUrl = storageClient.getSignedDownloadUrl(path, mediaType);
        }

        return Response.ok(MediaObject.newBuilder()
                .setId(mediaId)
                .setFullPath(path)
                .setMediaType(mediaType)
                .setSignedDownloadUrl(signedUrl == null ? "" : signedUrl.toString())
                .build())
                .build();
    }

    @POST
    @Path("/directUpload")
    public Response directUpload(@QueryParam("ext") final String fileExtension,
                                 @QueryParam("hasDownloadUrl") final boolean hasDownloadUrl,
                                 final byte[] body) {
        RequestValidator.validateNotBlank(fileExtension, "fileExtension");
        RequestValidator.validateNotEmpty(body);

        String mediaId = UUID.randomUUID().toString() + "." + fileExtension.toLowerCase();
        String mediaType = determineMediaType(fileExtension);
        String path = MediaUtils.toGcsPath(mediaId);
        LOGGER.info("directUpload mediaId={}, path={}, mediaType={}, bytes.length={}", mediaId, path, mediaType, body.length);

        storageClient.write(path, mediaType, body);

        URL signedUrl = null;
        if (hasDownloadUrl) {
            signedUrl = storageClient.getSignedDownloadUrl(path, mediaType);
        }

        return Response.ok(MediaObject.newBuilder()
                .setId(mediaId)
                .setFullPath(path)
                .setMediaType(mediaType)
                .setSignedDownloadUrl(signedUrl == null ? "" : signedUrl.toString())
                .build())
                .build();
    }

    @GET
    @Path("/directDownload")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    public Response directDownload(@QueryParam("mediaId") final String mediaId) {
        RequestValidator.validateNotBlank(mediaId, "mediaId");
        String path = MediaUtils.toGcsPath(mediaId);
        byte[] bytes = storageClient.read(path);
        if (bytes == null) {
            throw new NotFoundException();
        }

        return Response.ok(bytes).build();
    }


    @GET
    @Path("/url/download")
    public Response getSignedDownloadUrl(@QueryParam("mediaId") final String mediaId) {
        RequestValidator.validateNotBlank(mediaId, "mediaId");

        String path = MediaUtils.toGcsPath(mediaId);
        String mediaType = determineMediaType(path);
        LOGGER.info("upload mediaId={}, path={}, mediaType={}", mediaId, path, mediaType);

        URL signedUrl = storageClient.getSignedDownloadUrl(path, mediaType);
        return Response.ok(MediaObject.newBuilder()
                .setId(mediaId)
                .setFullPath(path)
                .setSignedDownloadUrl(signedUrl.toString())
                .setMediaType(mediaType)
                .build())
                .build();
    }
}
