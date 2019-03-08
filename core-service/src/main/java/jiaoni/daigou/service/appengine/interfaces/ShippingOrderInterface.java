package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.access.db.PageToken;
import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.wiremodel.Price;
import jiaoni.daigou.service.appengine.impls.customer.CustomerFacade;
import jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient;
import jiaoni.daigou.service.appengine.impls.products.ProductFacade;
import jiaoni.daigou.service.appengine.impls.shippingorders.ShippingOrderFacade;
import jiaoni.daigou.wiremodel.api.InitShippingOrderRequest;
import jiaoni.daigou.wiremodel.entity.Customer;
import jiaoni.daigou.wiremodel.entity.Product;
import jiaoni.daigou.wiremodel.entity.ShippingOrder;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/shippingOrders")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed( {Roles.ADMIN})
public class ShippingOrderInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingOrderInterface.class);

    private final CustomerFacade customerFacade;
    private final ProductFacade productFacade;
    private final ShippingOrderDbClient shippingOrderDbClient;
    private final ShippingOrderFacade shippingOrderFacade;

    @Inject
    public ShippingOrderInterface(final CustomerFacade customerFacade,
                                  final ProductFacade productFacade,
                                  final ShippingOrderDbClient shippingOrderDbClient,
                                  final ShippingOrderFacade shippingOrderFacade) {
        this.customerFacade = customerFacade;
        this.productFacade = productFacade;
        this.shippingOrderDbClient = shippingOrderDbClient;
        this.shippingOrderFacade = shippingOrderFacade;
    }

    @POST
    @Path("/init")
    public Response initShippingOrder(final InitShippingOrderRequest request) {
        RequestValidator.validateNotNull(request);
        RequestValidator.validateNotBlank(request.getReceiverCustomerId(), "customerId");

        LOGGER.info("init shipping order: " + ObjectMapperProvider.compactToJson(request));

        Customer receiver = null;//customerFacade.getCustomer(request.getReceiverCustomerId());
        if (receiver == null) {
            throw new NotFoundException("invalid customer ID: " + request.getReceiverCustomerId());
        }

        receiver = receiver.toBuilder()
                .clearAddresses()
                .addAddresses(request.getAddress())
                .build();

        ShippingOrder.Status status = request.getTotalWeightLb() == 0
                ? ShippingOrder.Status.INIT : ShippingOrder.Status.PACKED;

        Price totalPrice = Price.newBuilder()
                .setUnit(Price.Unit.USD)
                .setValue(request.getProductEntriesList()
                        .stream()
                        .map(t -> t.getSellPrice().getValue())
                        .reduce((a, b) -> a + b)
                        .get())
                .build();

        List<ShippingOrder.ProductEntry> productEntries = request.getProductEntriesList();
        productEntries = saveProductsIfNecessary(productEntries);

        ShippingOrder shippingOrder = ShippingOrder.newBuilder()
                .setStatus(status)
                .setCreationTime(System.currentTimeMillis())
                .setReceiver(receiver)
                .addAllProductEntries(productEntries)
                .setTotalWeightLb(request.getTotalWeightLb())
                .setTotalPrice(totalPrice)
                .setTotalSellPrice(request.getTotalSellPrice())
                .build();

        shippingOrder = shippingOrderDbClient.put(shippingOrder);

        return Response.ok(shippingOrder).build();
    }

    @GET
    @Path("/get/{id}")
    public Response getShippingOrderById(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id, "shippingOrderId");

        ShippingOrder shippingOrder = shippingOrderDbClient.getById(id);
        if (shippingOrder == null) {
            throw new NotFoundException();
        }

        return Response.ok(shippingOrder).build();
    }

    @DELETE
    @Path("/{id}/delete")
    public Response deleteShippingOrderById(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id, "shippingOrderId");
        shippingOrderDbClient.delete(id);
        return Response.ok().build();
    }

    @GET
    @Path("/query")
    public Response query(@QueryParam("customerId") final String customerId,
                          @QueryParam("pageToken") final String pageTokenStr,
                          @QueryParam("limit") final int limit) {
        PageToken pageToken = PageToken.fromPageToken(pageTokenStr);
        PaginatedResults<ShippingOrder> results;
        if (StringUtils.isNotBlank(customerId)) {
            results = shippingOrderFacade.queryByCustomerId(customerId, limit, pageToken);
        } else {
            results = shippingOrderFacade.queryAll(limit, pageToken);
        }
        return Response.ok(results).build();
    }

    private List<ShippingOrder.ProductEntry> saveProductsIfNecessary(final List<ShippingOrder.ProductEntry> entries) {
        List<ShippingOrder.ProductEntry> toReturn = new ArrayList<>();
        for (ShippingOrder.ProductEntry entry : entries) {
            Product product = entry.getProduct();
            if (StringUtils.isBlank(product.getId())) {
                product = productFacade.create(product);
            }
            toReturn.add(entry.toBuilder().setProduct(product).build());
        }
        return toReturn;
    }
}
