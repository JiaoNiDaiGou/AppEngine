package jiaoni.daigou.service.appengine.interfaces;

import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.appengine.access.db.PageToken;
import jiaoni.common.appengine.auth.Roles;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.wiremodel.Price;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.model.Order;
import jiaoni.daigou.service.appengine.impls.db.CustomerDbClient;
import jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient;
import jiaoni.daigou.service.appengine.impls.teddy.TeddyUtils;
import jiaoni.daigou.wiremodel.api.ExternalCreateShippingOrderRequest;
import jiaoni.daigou.wiremodel.api.InitShippingOrderRequest;
import jiaoni.daigou.wiremodel.entity.Customer;
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
import javax.inject.Named;
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

import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_CUSTOMER_ID;
import static jiaoni.daigou.service.appengine.impls.db.ShippingOrderDbClient.FIELD_STATUS;

@Path("/api/shippingOrders")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@RolesAllowed({ Roles.ADMIN })
public class ShippingOrderInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingOrderInterface.class);

    private final TeddyClient teddyClient;
    private final ShippingOrderDbClient shippingOrderDbClient;
    private final CustomerDbClient customerDbClient;

    @Inject
    public ShippingOrderInterface(@Named(TeddyAdmins.BY_ENV) final TeddyClient teddyClient,
                                  final ShippingOrderDbClient shippingOrderDbClient,
                                  final CustomerDbClient customerDbClient) {
        this.teddyClient = teddyClient;
        this.shippingOrderDbClient = shippingOrderDbClient;
        this.customerDbClient = customerDbClient;
    }

    @POST
    @Path("/init")
    public Response initShippingOrder(final InitShippingOrderRequest request) {
        RequestValidator.validateNotNull(request);
        RequestValidator.validateNotBlank(request.getReceiverCustomerId(), "customerId");

        LOGGER.info("init shipping order: " + ObjectMapperProvider.compactToJson(request));

        Customer receiver = customerDbClient.getById(request.getReceiverCustomerId());
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

        ShippingOrder shippingOrder = ShippingOrder.newBuilder()
                .setStatus(status)
                .setCreationTime(System.currentTimeMillis())
                .setReceiver(receiver)
                .addAllProductEntries(request.getProductEntriesList())
                .setTotalWeightLb(request.getTotalWeightLb())
                .setTotalPrice(totalPrice)
                .setTotalSellPrice(request.getTotalSellPrice())
                .build();

        shippingOrder = shippingOrderDbClient.put(shippingOrder);

        if (status == ShippingOrder.Status.PACKED) {
            LOGGER.info("Call teddy for {}", shippingOrder.getId());

            Order order = teddyClient.makeOrder(
                    TeddyUtils.convertToTeddyReceiver(shippingOrder.getReceiver()),
                    TeddyUtils.convertToTeddyProducts(shippingOrder.getProductEntriesList()),
                    shippingOrder.getTotalWeightLb()
            );

            shippingOrder = shippingOrder.toBuilder()
                    .setTeddyOrderId(String.valueOf(order.getId()))
                    .setTeddyFormattedId(order.getFormattedId())
                    .setCustomerNotified(false)
                    .setStatus(ShippingOrder.Status.EXTERNAL_SHIPPING_CREATED)
                    .build();

            shippingOrder = shippingOrderDbClient.put(shippingOrder);
        }

        return Response.ok(shippingOrder).build();
    }

    @POST
    @Path("/{id}/externalCreate")
    public Response externalCreateShippingOrder(@PathParam("id") final String id,
                                                final ExternalCreateShippingOrderRequest request) {
        RequestValidator.validateNotBlank(id, "shippingOrderId");
        RequestValidator.validateRequest(request.getTotalWeightLb() >= 0);

        ShippingOrder shippingOrder = shippingOrderDbClient.getById(id);
        if (shippingOrder == null) {
            throw new NotFoundException();
        }

        Order order = teddyClient.makeOrder(
                TeddyUtils.convertToTeddyReceiver(shippingOrder.getReceiver()),
                TeddyUtils.convertToTeddyProducts(shippingOrder.getProductEntriesList()),
                request.getTotalWeightLb()
        );

        shippingOrder = shippingOrder.toBuilder()
                .setTeddyOrderId(String.valueOf(order.getId()))
                .setTeddyFormattedId(order.getFormattedId())
                .setCustomerNotified(false)
                .setStatus(ShippingOrder.Status.EXTERNAL_SHIPPING_CREATED)
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
                          @QueryParam("delivered") final boolean delivered,
                          @QueryParam("pageToken") final String pageTokenStr,
                          @QueryParam("limit") final int limit) {
        PageToken pageToken = PageToken.fromPageToken(pageTokenStr);

        List<DbQuery> queries = new ArrayList<>();
        if (StringUtils.isNotBlank(customerId)) {
            queries.add(DbQuery.eq(FIELD_CUSTOMER_ID, customerId));
        }
        if (delivered) {
            queries.add(DbQuery.eq(FIELD_STATUS, ShippingOrder.Status.DELIVERED.name()));
        } else {
            queries.add(DbQuery.notEq(FIELD_STATUS, ShippingOrder.Status.DELIVERED.name()));
        }
        PaginatedResults<ShippingOrder> results = shippingOrderDbClient.queryInPagination(
                DbQuery.and(queries),
                limit,
                pageToken);
        return Response.ok(results).build();
    }
}
