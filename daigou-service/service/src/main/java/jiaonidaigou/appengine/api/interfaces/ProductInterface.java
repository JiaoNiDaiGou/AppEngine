package jiaonidaigou.appengine.api.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;
import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.InternalIOException;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaonidaigou.appengine.api.impls.ProductDbClient;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
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

    private static final int DEFAULT_PAGE_LIMIT = 100;

    private final ProductDbClient dbClient;

    @Inject
    public ProductInterface(final ProductDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @GET
    @Path("/getAll")
    public Response getAll(@PathParam("app") final String appName) {
        List<Product> products = dbClient.scan().collect(Collectors.toList());
        return Response.ok(products).build();
    }

    @POST
    @Path("/create")
    public Response createProduct(final Product product) {
        RequestValidator.validateNotNull(product);
        Product createdProduct = dbClient.put(product);
        return Response.ok(createdProduct).build();
    }

    @POST
    @Path("/update")
    public Response updateProduct(final Product product) {
        RequestValidator.validateNotNull(product);
        RequestValidator.validateNotBlank(product.getId());
        Product createdProduct = dbClient.put(product);
        return Response.ok(createdProduct).build();
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
