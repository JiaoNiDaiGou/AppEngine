package jiaoni.common.appengine.utils;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

public class ExceptionMappingFeature implements Feature {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMappingFeature.class);

    @Override
    public boolean configure(final FeatureContext context) {
        context.register(JsonExceptionMapper.class, ExceptionMapper.class);
        context.register(WebApplicationExceptionMapper.class, ExceptionMapper.class);
        context.register(GenericExceptionMapper.class, ExceptionMapper.class);
        return true;
    }

    /**
     * Handle generic exceptions.
     * Maps to 500.
     */
    @Provider
    public static class GenericExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(final Exception exception) {
            LOGGER.error("system error.", exception);
            return Response.serverError().entity(exception.getMessage()).build();
        }
    }

    /**
     * Handles web application exception(provided by jersey).
     * Maps them to error.status.
     */
    @Provider
    public static class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(final WebApplicationException exception) {
            LOGGER.error("system error.", exception);
            return exception.getResponse();
        }
    }

    /**
     * Handles json deserialization error for the request.
     * Maps them to 400.
     */
    @Provider
    public static class JsonExceptionMapper implements ExceptionMapper<JsonMappingException> {
        @Override
        public Response toResponse(JsonMappingException exception) {
            LOGGER.warn("input error.", exception);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}
