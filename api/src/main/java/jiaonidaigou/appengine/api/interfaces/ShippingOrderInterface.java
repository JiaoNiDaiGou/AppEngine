package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.api.access.db.core.PageToken;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.guice.JiaoNiDaiGou;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.wiremodel.api.InitShippingOrderRequest;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import jiaonidaigou.appengine.wiremodel.entity.ShippingOrder;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    public ShippingOrderInterface(@Named(TeddyAdmins.HACK) final TeddyClient teddyClient,
                                  final ShippingOrderDbClient shippingOrderDbClient,
                                  @JiaoNiDaiGou final CustomerDbClient customerDbClient) {
        this.teddyClient = teddyClient;
        this.shippingOrderDbClient = shippingOrderDbClient;
        this.customerDbClient = customerDbClient;
    }

    @POST
    @Path("/init")
    public Response initShippingOrder(final InitShippingOrderRequest request) {
        RequestValidator.validateNotNull(request);
        RequestValidator.validateNotBlank(request.getReceiverCustomerId(), "customerId");

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

        ShippingOrder shippingOrder = ShippingOrder.newBuilder()
                .setStatus(status)
                .setCreationTime(System.currentTimeMillis())
                .setReceiver(receiver)
                .addAllProductEntries(request.getProductEntriesList())
                .setTotalWeightLb(request.getTotalWeightLb())
                .build();

        shippingOrder = shippingOrderDbClient.put(shippingOrder);

        return Response.ok(shippingOrder).build();
    }

    @POST
    @Path("/pack")
    public Response packShippingOrder(@QueryParam("id") final String id,
                                      @QueryParam("totalWeightLb") final double totalWeightLb) {
        RequestValidator.validateNotBlank(id, "shippingOrderId");
        RequestValidator.validateRequest(totalWeightLb > 0, "weight (lb) must be greater than 0.");

        ShippingOrder shippingOrder = shippingOrderDbClient.getById(id);
        if (shippingOrder == null) {
            throw new NotFoundException("invalid shipping order ID: " + id);
        }

        shippingOrder = shippingOrder.toBuilder()
                .setTotalWeightLb(totalWeightLb)
                .build();
        shippingOrder = shippingOrderDbClient.put(shippingOrder);

        return Response.ok(shippingOrder).build();
    }

    @GET
    @Path("/get")
    public Response getShippingOrderById(@QueryParam("id") final String id) {
        RequestValidator.validateNotBlank(id, "shippingOrderId");

        ShippingOrder shippingOrder = shippingOrderDbClient.getById(id);
        if (shippingOrder == null) {
            throw new NotFoundException();
        }

        return Response.ok(shippingOrder).build();
    }

    @DELETE
    @Path("/delete")
    public Response deleteShippingOrderById(@QueryParam("id") final String id) {
        RequestValidator.validateNotBlank(id, "shippingOrderId");
        shippingOrderDbClient.delete(id);
        return Response.ok().build();
    }

    @GET
    @Path("/query")
    public Response queryShippingOrders(@QueryParam("customerId") final String customerId,
                                        @QueryParam("customerName") final String customerName,
                                        @QueryParam("customerPhone") final String customerPhone,
                                        @QueryParam("includeDelivered") final boolean includeDelivered,
                                        @QueryParam("pageToken") final String pageTokenStr,
                                        @QueryParam("limit") final int limit) {
        if (includeDelivered) {
            throw new NotSupportedException("includeDelivered not supported yet");
        }

        PageToken pageToken = PageToken.fromPageToken(pageTokenStr);
        RequestValidator.validateRequest(pageToken == null || pageToken.isSourceInMemory());

        List<ShippingOrder> shippingOrders = shippingOrderDbClient.queryNonDeliveredOrders()
                .stream()
                .filter(t -> {
                    if (StringUtils.isNotBlank(customerId) && !t.getReceiver().getId().equals(customerId)) {
                        return false;
                    }

                    if (StringUtils.isNotBlank(customerName) && !t.getReceiver().getName().equals(customerName)) {
                        return false;
                    }

                    if (StringUtils.isNotBlank(customerPhone) && !t.getReceiver().getPhone().getPhone().equals(customerPhone)) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        int from = pageToken == null ? 0 : pageToken.getIndex();
        int to = limit > 0 ? Math.min(shippingOrders.size(), from + limit) : shippingOrders.size();
        String newPageToken = to == shippingOrders.size() ? null : PageToken.inMemory(to).toPageToken();
        if (from != 0 || to != shippingOrders.size()) {
            shippingOrders = shippingOrders.subList(from, to);
        }

        PaginatedResults<ShippingOrder> toReturn = PaginatedResults
                .<ShippingOrder>builder()
                .withTotoalCount(shippingOrders.size())
                .withResults(shippingOrders)
                .withPageToken(newPageToken)
                .build();
        return Response.ok(toReturn).build();
    }
}
