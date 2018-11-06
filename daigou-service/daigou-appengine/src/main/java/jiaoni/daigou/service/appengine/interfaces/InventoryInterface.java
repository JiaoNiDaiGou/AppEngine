package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.daigou.service.appengine.impls.db.InventoryDbClient;
import jiaoni.daigou.service.appengine.impls.products.ProductFacade;
import jiaoni.daigou.wiremodel.entity.InventoryItem;
import jiaoni.daigou.wiremodel.entity.Product;
import org.apache.commons.lang3.tuple.Triple;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class InventoryInterface {
    private final InventoryDbClient inventoryDbClient;
    private final ProductFacade productAccess;

    @Inject
    public InventoryInterface(final InventoryDbClient inventoryDbClient,
                              final ProductFacade productAccess) {
        this.inventoryDbClient = inventoryDbClient;
        this.productAccess = productAccess;
    }

    @GET
    @Path("/all")
    public Response getAll() {
        List<Triple<String, Integer, Long>> inventoryRecords = inventoryDbClient.scan().collect(Collectors.toList());
        Map<String, Product> products = productAccess.getAll().stream().collect(Collectors.toMap(Product::getId, t -> t));
        List<InventoryItem> inventoryItems = inventoryRecords
                .stream()
                .map(t -> {
                    Product product = products.get(t.getLeft());
                    if (product == null) {
                        return null;
                    }
                    return InventoryItem.newBuilder()
                            .setProduct(product)
                            .setQuantity(t.getMiddle())
                            .setLastUpdateTime(t.getRight())
                            .build();
                })
                .collect(Collectors.toList());
        return Response.ok(inventoryItems).build();
    }

    @POST
    @Path("/create")
    public Response createWithProduct(final InventoryItem inventoryItem) {
        RequestValidator.validateNotNull(inventoryItem);

        Product product = productAccess.create(inventoryItem.getProduct());

        Triple<String, Integer, Long> record =
                Triple.of(product.getId(), inventoryItem.getQuantity(), System.currentTimeMillis());
        InventoryItem afterCreate = InventoryItem.newBuilder()
                .setProduct(product)
                .setQuantity(record.getMiddle())
                .setLastUpdateTime(record.getRight())
                .build();
        return Response.ok(afterCreate).build();
    }

    @POST
    @Path("/update")
    public Response updateProduct(@QueryParam("productId") final String productId,
                                  @QueryParam("quantity") final int quantity) {
        RequestValidator.validateNotBlank(productId);
        RequestValidator.validateRequest(quantity >= 0);

        Product product = productAccess.get(productId);
        if (product == null) {
            throw new NotFoundException();
        }

        if (quantity == 0) {
            inventoryDbClient.delete(productId);
            return Response.ok().build();
        } else {
            Triple<String, Integer, Long> record = Triple.of(productId, quantity, System.currentTimeMillis());
            InventoryItem afterCreate = InventoryItem.newBuilder()
                    .setProduct(product)
                    .setQuantity(record.getMiddle())
                    .setLastUpdateTime(record.getRight())
                    .build();
            return Response.ok(afterCreate).build();
        }
    }
}
