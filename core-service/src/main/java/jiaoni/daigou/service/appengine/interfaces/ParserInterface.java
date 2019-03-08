package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.daigou.service.appengine.impls.parser.ParserFacade;
import jiaoni.daigou.wiremodel.api.ParseRequest;
import jiaoni.daigou.wiremodel.api.ParseResponse;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/parse")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ParserInterface {
    private final ParserFacade aggregateParser;

    @Inject
    public ParserInterface(final ParserFacade aggregateParser) {
        this.aggregateParser = aggregateParser;
    }

    @POST
    public Response parse(final ParseRequest parseRequest) {
        RequestValidator.validateNotNull(parseRequest, "parseRequest");
        ParseResponse parseResponse = aggregateParser.parse(parseRequest);
        return Response.ok(parseResponse).build();
    }
}
