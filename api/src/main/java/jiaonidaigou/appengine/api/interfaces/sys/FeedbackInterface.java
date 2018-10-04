package jiaonidaigou.appengine.api.interfaces.sys;

import jiaonidaigou.appengine.api.access.db.FeedbackDbClient;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.wiremodel.entity.sys.Feedback;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/sys/feedback")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class FeedbackInterface {
    private final FeedbackDbClient dbClient;

    @Inject
    public FeedbackInterface(final FeedbackDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @GET
    @Path("/get/allOpen")
    public Response getOpenFeedback() {
        List<Feedback> toReturn = dbClient.getAllOpenFeedbacks();
        return Response.ok(toReturn).build();
    }

    @POST
    @Path("/{id}/close")
    public Response close(@PathParam("id") final String id) {
        Feedback feedback = dbClient.getById(id);
        if (feedback == null) {
            throw new NotFoundException();
        }
        if (!feedback.getOpen()) {
            return Response.notModified().build();
        }
        feedback = feedback.toBuilder()
                .setOpen(false)
                .setTimestamp(System.currentTimeMillis())
                .build();
        feedback = dbClient.put(feedback);
        return Response.ok(feedback).build();
    }

    @POST
    @Path("/post")
    public Response post(final Feedback feedback) {
        RequestValidator.validateNotNull(feedback);
        RequestValidator.validateNotBlank(feedback.getContent());
        RequestValidator.validateRequest(StringUtils.isBlank(feedback.getId()));
        Feedback toSave = Feedback.newBuilder()
                .setOpen(true)
                .setRequesterName(StringUtils.isNotBlank(feedback.getRequesterName()) ? feedback.getRequesterName() : "anonymous")
                .setContent(feedback.getContent())
                .setTimestamp(System.currentTimeMillis())
                .build();
        Feedback afterSave = dbClient.put(toSave);
        return Response.ok(afterSave).build();
    }
}
