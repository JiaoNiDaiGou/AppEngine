package jiaoni.common.appengine.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Priority(FilterPriorities.WIRE_LOG)
@Provider
public class WireLogFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireLogFilter.class);

    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {
        LOGGER.info("Headers: " + requestContext.getHeaders());
        LOGGER.info("PathParams: " + requestContext.getUriInfo().getPathParameters());
        LOGGER.info("QueryParams: " + requestContext.getUriInfo().getQueryParameters());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) {
    }
}
