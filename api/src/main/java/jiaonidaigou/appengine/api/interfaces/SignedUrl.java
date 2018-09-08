package jiaonidaigou.appengine.api.interfaces;

import com.google.inject.Inject;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.common.utils.Environments;
import org.jvnet.hk2.annotations.Service;

import java.io.UnsupportedEncodingException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/signed_url")
@Produces(MediaType.TEXT_PLAIN)
@Service
public class SignedUrl {
    private static final String PATH_BASE = Environments.GCS_ROOT_ENDSLASH + "test/";
    private static final com.google.common.net.MediaType DEFAULT_MEDIA_TYPE = com.google.common.net.MediaType.OCTET_STREAM;

    private final StorageClient storageClient;

    @Inject
    public SignedUrl(final StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @GET
    @Path("/upload")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUploadSignedUrl(@QueryParam("path") final String path) {
        String fullPath = PATH_BASE + path;
        try {
            String signedUrl = storageClient.getSignedUploadUrl(path, DEFAULT_MEDIA_TYPE);
            return Response.ok(signedUrl).build();
        } catch (UnsupportedEncodingException e) {
            throw new InternalServerErrorException();
        }
    }

    @GET
    @Path("/signedDownloadUrl")
    public Response getSignedDownloadUrl(@QueryParam("path") final String path) {
        String fullPath = PATH_BASE + path;
        try {
            String signedUrl = storageClient.getSignedDownloadUrl(fullPath, DEFAULT_MEDIA_TYPE);
            return Response.ok(signedUrl).build();
        } catch (UnsupportedEncodingException e) {
            throw new InternalServerErrorException();
        }
    }
}
