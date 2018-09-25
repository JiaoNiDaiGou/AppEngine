package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.db.ProductDbClient;
import jiaonidaigou.appengine.api.guice.JiaoNiDaiGou;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.wiremodel.entity.Product;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/{app}/products")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
//@RolesAllowed({ Roles.ADMIN })
public class ProductInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductInterface.class);

    private static final int DEFAULT_PAGE_LIMIT = 100;

    private final ProductDbClient dbClient;

    @Inject
    public ProductInterface(@JiaoNiDaiGou final ProductDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @GET
    @Path("/getAll")
    public Response getAll(@PathParam("app") final String appName) {
        LOGGER.info("appName:" + appName);
        List<Product> products = dbClient.scan().collect(Collectors.toList());
        return Response.ok(products).build();
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
