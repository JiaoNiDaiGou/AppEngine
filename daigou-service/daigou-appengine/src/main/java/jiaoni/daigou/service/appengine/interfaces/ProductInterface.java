package jiaoni.daigou.service.appengine.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;
import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.daigou.service.appengine.impls.ProductAccess;
import jiaoni.daigou.wiremodel.entity.Product;
import org.apache.commons.collections4.CollectionUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ProductInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductInterface.class);

    private final ProductAccess access;

    @Inject
    public ProductInterface(final ProductAccess access) {
        this.access = access;
    }

    @GET
    @Path("/all")
    public Response getAll() {
        return Response.ok(access.getAll()).build();
    }

    @POST
    @Path("/create")
    public Response createProduct(final Product product) {
        RequestValidator.validateNotNull(product);
        return Response.ok(access.create(product)).build();
    }

    @POST
    @Path("/update")
    public Response updateProduct(final Product product) {
        RequestValidator.validateNotNull(product);
        Product toReturn = access.update(product);
        if (toReturn == null) {
            throw new NotFoundException();
        }
        return Response.ok(toReturn).build();
    }

    @POST
    @Path("/{id}/attachMedia")
    public Response attachMedia(@PathParam("id") final String id,
                                final List<String> mediaIds) {
        RequestValidator.validateNotBlank(id);
        RequestValidator.validateRequest(CollectionUtils.isNotEmpty(mediaIds));
        Product product = access.attachMedia(id, mediaIds);
        if (product == null) {
            throw new NotFoundException();
        }
        return Response.ok(product).build();
    }

    @GET
    @Path("/hints")
    public Response getProductsHints() {
        JsonNode jsonNode;
        try {
            jsonNode = ObjectMapperProvider.get()
                    .readTree(Resources.getResource("products_hints.json"));
            LOGGER.info("Load {} product hints", jsonNode.size());
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        return Response.ok(jsonNode).build();
    }
}
