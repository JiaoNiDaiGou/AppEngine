package jiaonidaigou.appengine.api.interfaces;

import jiaonidaigou.appengine.api.access.db.ShippingOrderDbClient;
import jiaonidaigou.appengine.api.access.db.core.PageToken;
import jiaonidaigou.appengine.api.auth.Roles;
import jiaonidaigou.appengine.api.utils.RequestValidator;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.model.Order;
import jiaonidaigou.appengine.lib.teddy.model.Product;
import jiaonidaigou.appengine.lib.teddy.model.Receiver;
import jiaonidaigou.appengine.wiremodel.api.CreateShippingOrderRequest;
import jiaonidaigou.appengine.wiremodel.api.CreateShippingOrderResponse;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import jiaonidaigou.appengine.wiremodel.entity.ProductCategory;
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

    @Inject
    public ShippingOrderInterface(@Named(TeddyAdmins.HACK) final TeddyClient teddyClient,
                                  final ShippingOrderDbClient shippingOrderDbClient) {
        this.teddyClient = teddyClient;
        this.shippingOrderDbClient = shippingOrderDbClient;
    }

    @POST
    @Path("/create")
    public Response createShippingOrder(final CreateShippingOrderRequest request) {
        RequestValidator.validateNotNull(request);

        Order order = teddyClient.makeOrder(
                toTeddyReceiver(request),
                toTeddyProducts(request),
                request.getTotalWeightLb());

        ShippingOrder shippingOrder = ShippingOrder.newBuilder()
                .setStatus(ShippingOrder.Status.CREATED)
                .setTeddyOrderId(order.getId() + "|" + order.getFormattedId())
                .setCreationTime(System.currentTimeMillis())
                .setReceiver(request.getReceiver())
                .addAllProductEntries(request.getProductEntriesList())
                .setTotalWeightLb(request.getTotalWeightLb())
                .build();

        shippingOrder = shippingOrderDbClient.put(shippingOrder);

        CreateShippingOrderResponse response = CreateShippingOrderResponse.newBuilder()
                .setShippingOrder(shippingOrder)
                .build();

        return Response.ok(response).build();
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

    private static Receiver toTeddyReceiver(final CreateShippingOrderRequest request) {
//        return Receiver.builder()
//                .withName(request.getReceiver().getName())
//                .withUserId(request.getReceiver().getSocialContacts().getTeddyUserId())
//                .withPhone(request.getReceiver().getPhone().getPhone())
//                .withAddressRegion(request.getReceiver().getAddress().getRegion())
//                .withAddressCity(request.getReceiver().getAddress().getCity())
//                .withAddressZone(request.getReceiver().getAddress().getZone())
//                .withAddress(request.getReceiver().getAddress().getAddress())
//                .build();
        return null;
    }

    // TODO:
    // Move to TeddyUtils

    private static List<Product> toTeddyProducts(final CreateShippingOrderRequest request) {
        return request.getProductEntriesList()
                .stream()
                .map(ShippingOrderInterface::toTeddyProduct)
                .collect(Collectors.toList());
    }

    private static Product toTeddyProduct(final ShippingOrder.ProductEntry productEntry) {
        return Product.builder()
                .withBrand(productEntry.getProduct().getBrand())
                .withCategory(toTeddyProductCategory(productEntry.getProduct().getCategory()))
                .withName(productEntry.getProduct().getName())
                .withQuantity(productEntry.getQuantity())
                .withUnitPriceInDollers((int) productEntry.getSellPrice().getValue())
                .build();
    }

    private static Product.Category toTeddyProductCategory(final ProductCategory category) {
        switch (category) {
            case BAGS:
                return Product.Category.BAGS;
            case FOOD:
                return Product.Category.FOOD;
            case TOYS:
            case DAILY_NECESSITIES:
                return Product.Category.TOYS_AND_DAILY_NECESSITIES;
            case SHOES:
            case CLOTHES:
                return Product.Category.CLOTHES_AND_SHOES;
            case MAKE_UP:
                return Product.Category.MAKE_UP;
            case WATCHES:
            case ACCESSORIES:
                return Product.Category.WATCH_AND_ACCESSORIES;
            case LARGE_ITEMS:
                return Product.Category.LARGE_ITEMS;
            case MILK_POWDER:
                return Product.Category.MILK_POWDER;
            case BABY_PRODUCTS:
                return Product.Category.BABY_PRODUCTS;
            case SMALL_APPLIANCES:
                return Product.Category.SMALL_APPLIANCES;
            case HEALTH_SUPPLEMENTS:
                return Product.Category.HEALTH_SUPPLEMENTS;
            case LARGE_COMMERCIAL_GOODS:
                return Product.Category.LARGE_COMMERCIAL_GOODS;
            case UNKNOWN:
            case UNRECOGNIZED:
            default:
                throw new IllegalStateException("unexpected product category");
        }
    }
}
