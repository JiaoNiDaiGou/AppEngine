package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.db.ProductDbClient;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ProductInterface {
    private final ProductDbClient dbClient;

    @Inject
    public ProductInterface(final ProductDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @POST
    @Path("/create")
    public Response createProduct(final Product product) {
        RequestValidator.validateNotNull(product);
        RequestValidator.validateEmpty(product.getId());
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
}
