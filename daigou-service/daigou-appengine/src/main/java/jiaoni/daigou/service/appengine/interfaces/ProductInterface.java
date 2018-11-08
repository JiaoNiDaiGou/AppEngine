package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.daigou.service.appengine.impls.products.ProductFacade;
import jiaoni.daigou.service.appengine.impls.products.ProductHintsFacade;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.wiremodel.common.entity.ProductsHints;
import org.apache.commons.collections4.CollectionUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final ProductFacade productFacade;
    private final ProductHintsFacade productHintsFacade;

    @Inject
    public ProductInterface(final ProductFacade productFacade,
                            final ProductHintsFacade productHintsFacade) {
        this.productFacade = productFacade;
        this.productHintsFacade = productHintsFacade;
    }

    @GET
    @Path("/all")
    public Response getAll() {
        return Response.ok(productFacade.getAll()).build();
    }

    @POST
    @Path("/create")
    public Response createProduct(final Product product) {
        RequestValidator.validateNotNull(product);
        return Response.ok(productFacade.create(product)).build();
    }

    @POST
    @Path("/update")
    public Response updateProduct(final Product product) {
        RequestValidator.validateNotNull(product);
        Product toReturn = productFacade.update(product);
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
        Product product = productFacade.attachMedia(id, mediaIds);
        if (product == null) {
            throw new NotFoundException();
        }
        return Response.ok(product).build();
    }

    @GET
    @Path("/hints")
    public Response getProductsHints() {
        ProductsHints hints = productHintsFacade.loadHints();
        return Response.ok(hints).build();
    }
}
