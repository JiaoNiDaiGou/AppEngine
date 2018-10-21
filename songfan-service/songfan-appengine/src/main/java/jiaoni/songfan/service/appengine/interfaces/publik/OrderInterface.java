package jiaoni.songfan.service.appengine.interfaces.publik;

import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.Price;
import jiaoni.songfan.service.appengine.impls.BraintreeAccess;
import jiaoni.songfan.service.appengine.impls.CustomerDbClient;
import jiaoni.songfan.service.appengine.impls.MenuDbClient;
import jiaoni.songfan.service.appengine.impls.OrderDbClient;
import jiaoni.songfan.wiremodel.api.ClientTokenResponse;
import jiaoni.songfan.wiremodel.api.InitOrderRequest;
import jiaoni.songfan.wiremodel.entity.Customer;
import jiaoni.songfan.wiremodel.entity.Dish;
import jiaoni.songfan.wiremodel.entity.Menu;
import jiaoni.songfan.wiremodel.entity.Order;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static jiaoni.songfan.wiremodel.entity.Order.Status.INIT;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
@PermitAll
public class OrderInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderInterface.class);

    private final CustomerDbClient customerDbClient;
    private final MenuDbClient menuDbClient;
    private final OrderDbClient orderDbClient;
    private final BraintreeAccess braintreeAccess;

    @Inject
    public OrderInterface(final CustomerDbClient customerDbClient,
                          final MenuDbClient menuDbClient,
                          final OrderDbClient orderDbClient,
                          final BraintreeAccess braintreeAccess) {
        this.customerDbClient = customerDbClient;
        this.menuDbClient = menuDbClient;
        this.orderDbClient = orderDbClient;
        this.braintreeAccess = braintreeAccess;
    }

    @GET
    @Path("/braintreeClientToken")
    public Response getBraintreeClientToken(@QueryParam("customerId") final String customerId) {
        String clientToken = braintreeAccess.getClientToken(customerId);
        return Response.ok(ClientTokenResponse.newBuilder().setClientToken(clientToken).build()).build();
    }


    @POST
    @Path("/init")
    public Response init(final InitOrderRequest request) {
        RequestValidator.validateNotNull(request);
        RequestValidator.validateNotBlank(request.getMenuId(), "menuId");
        RequestValidator.validateRequest(request.getDeliveryAddressIndex() >= 0);
        RequestValidator.validateNotEmpty(request.getDishesMap());
        RequestValidator.validateNotBlank(request.getPaymentNonce(), "paymentNonce");
        validateCustomer(request);

        Menu menu = menuDbClient.getById(request.getMenuId());
        if (menu == null) {
            throw new NotFoundException();
        }
        LOGGER.info("Load menu {}", menu);

        RequestValidator.validateNotNull(request.getDeliveryAddressIndex() < menu.getDeliveryAddressesCount());
        Address deliveryAddress = menu.getDeliveryAddresses(request.getDeliveryAddressIndex());

        Map<String, Dish> dishesById = new HashMap<>();
        Map<String, Price> dishPricesById = new HashMap<>();
        for (Menu.MenuEntry entry : menu.getMenuEntriesList()) {
            dishesById.put(entry.getDish().getId(), entry.getDish());
            dishPricesById.put(entry.getDish().getId(), entry.getPrice());
        }

        Customer customer = getOrCreateCustomer(request, deliveryAddress);

        double totalPriceBeforeTaxVal = 0d;
        List<Order.OrderEntry> orderEntries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : request.getDishesMap().entrySet()) {
            Dish dish = dishesById.get(entry.getKey());
            RequestValidator.validateNotNull(dish);
            RequestValidator.validateRequest(entry.getValue() > 0);
            Price price = dishPricesById.get(entry.getKey());
            totalPriceBeforeTaxVal += price.getValue() * entry.getValue();
            orderEntries.add(Order.OrderEntry.newBuilder()
                    .setDish(dish)
                    .setAmount(entry.getValue())
                    .setUnitPrice(price)
                    .build());
        }
        Price totalPriceBeforeTax = Price.newBuilder().setUnit(Price.Unit.USD).setValue(totalPriceBeforeTaxVal).build();

        final long now = DateTime.now().getMillis();
        Order order = Order.newBuilder()
                .setStatus(INIT)
                .setCustomer(customer)
                .setDeliveryAddress(deliveryAddress)
                .addAllOrderEntries(orderEntries)
                .setTotalPriceBeforeTax(totalPriceBeforeTax)
                .setCreationTime(now)
                .setLastUpdatedTime(now)
                .addTags(menu.getId())
                .addTags(todayDate())
                .build();
        order = orderDbClient.put(order);
        LOGGER.info("Save order {}", order.getId());

        BraintreeAccess.TransactionResult transactionResult = makeTransaction(
                customer.getId(),
                totalPriceBeforeTax,
                request.getPaymentType(),
                request.getPaymentNonce());
        if (transactionResult.isSuccess()) {
            // TODO: track transactionid
            order = order.toBuilder()
                    .setTransactionId(transactionResult.getTransactionId())
                    .setStatus(Order.Status.PAID)
                    .setLastUpdatedTime(System.currentTimeMillis())
                    .build();
            order = orderDbClient.put(order);
        }

        return Response.ok(order).build();
    }

    private BraintreeAccess.TransactionResult makeTransaction(final String customerId,
                                                              final Price price,
                                                              final String type,
                                                              final String nonce) {
        switch (BraintreeAccess.paymentMethodOf(type)) {
            case CREDIT_CARD:
                return braintreeAccess.creditCardTransaction(price, nonce);
            default:
                throw new BadRequestException("unknown payment method");
        }
    }

    private Customer getOrCreateCustomer(final InitOrderRequest request,
                                         final Address deliveryAddress) {
        switch (request.getCustomerObjCase()) {
            case CUSTOMER_ID: {
                LOGGER.info("Load customer {}", request.getCustomerId());
                Customer customer = customerDbClient.getById(request.getCustomerId());
                if (customer == null) {
                    throw new NotFoundException();
                }
                return customer;
            }
            case CUSTOMER: {
                Customer customer = request.getCustomer()
                        .toBuilder()
                        .setId(CustomerDbClient.computeKey(request.getCustomer()))
                        .clearAddresses()
                        .addAddresses(deliveryAddress)
                        .setCreationTime(System.currentTimeMillis())
                        .build();
                customer = customerDbClient.putAndUpdateTimestamp(customer);
                LOGGER.info("Create customer {}", customer);
                return customer;
            }
            default:
                throw new BadRequestException();
        }
    }

    private static String todayDate() {
        return new LocalDateTime().toString("yyyy-MM-dd");
    }

    private static void validateCustomer(final InitOrderRequest request) {
        if (StringUtils.isBlank(request.getCustomerId())) {
            Customer customer = request.getCustomer();
            RequestValidator.validateRequest(
                    StringUtils.isNotBlank(customer.getPhone().getPhone()) || StringUtils.isNotBlank(customer.getEmail())
            );
        }
    }
}
