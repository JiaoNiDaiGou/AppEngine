package jiaonidaigou.appengine.api.filters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Priority(FilterPriorities.CORS)
@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Set<String> ALLOWED_ORIGINS = ImmutableSet.of(
            // TODO:
            // Move to properties
            //
            // Add trusted clients.
            "http://localhost:3000",
            "https://localhost:3000");

    private static final List<String> ALLOWED_CONTROL_HEADERS = ImmutableList.of(
            "Authorization",
            "Content-Type"
    );

    private static void putIfNotPresent(MultivaluedMap<String, Object> h, String header, String value) {
        if (!h.containsKey(header)) {
            h.putSingle(header, value);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        if (requestContext.getMethod().equals("OPTIONS")) {
            requestContext.abortWith(Response.ok().build());
        }
    }

    private List<String> getAllowedControlHeaders() {
        return ALLOWED_CONTROL_HEADERS;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String incomingOrigin = request.getHeaderString("Origin");
        if (isAllowedOrigin(incomingOrigin)) {
            MultivaluedMap<String, Object> responseHeaders = response.getHeaders();
            putIfNotPresent(responseHeaders, "Access-Control-Allow-Origin", incomingOrigin);
            putIfNotPresent(responseHeaders, "Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
            putIfNotPresent(responseHeaders, "Access-Control-Max-Age", "1728000");
            putIfNotPresent(responseHeaders, "Access-Control-Allow-Headers", String.join(", ", getAllowedControlHeaders()));
            putIfNotPresent(responseHeaders, "Access-Control-Expose-Headers", "Location, Link");
            putIfNotPresent(responseHeaders, "Access-Control-Allow-Credentials", "true");
        }
    }

    private boolean isAllowedOrigin(final String origin) {
        if (ALLOWED_ORIGINS.contains("*")) {
            return true;
        }
        return ALLOWED_ORIGINS.contains(origin);
    }
}
