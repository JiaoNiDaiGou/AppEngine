package jiaoni.songfan.service.appengine.interfaces;

import com.sun.org.apache.xalan.internal.xsltc.runtime.InternalRuntimeError;
import jiaoni.common.appengine.utils.RequestValidator;
import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.Price;
import jiaoni.songfan.service.appengine.impls.CustomerDbClient;
import jiaoni.songfan.service.appengine.impls.MenuDbClient;
import jiaoni.songfan.service.appengine.impls.OrderDbClient;
import jiaoni.songfan.wiremodel.api.InitOrderRequest;
import jiaoni.songfan.wiremodel.entity.Combo;
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

import static jiaoni.songfan.wiremodel.entity.Order.Status.INIT;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class OrderInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderInterface.class);

    private final CustomerDbClient customerDbClient;
    private final MenuDbClient menuDbClient;
    private final OrderDbClient orderDbClient;

    @Inject
    public OrderInterface(final CustomerDbClient customerDbClient,
                          final MenuDbClient menuDbClient,
                          final OrderDbClient orderDbClient) {
        this.customerDbClient = customerDbClient;
        this.menuDbClient = menuDbClient;
        this.orderDbClient = orderDbClient;
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") final String id) {
        RequestValidator.validateNotBlank(id);
        Order order = orderDbClient.getById(id);
        if (order == null) {
            throw new NotFoundException();
        }
        return Response.ok(order).build();
    }

    @POST
    @Path("/init")
    public Response init(final InitOrderRequest request) {
        RequestValidator.validateNotNull(request);
        RequestValidator.validateNotBlank(request.getMenuId(), "menuId");
        RequestValidator.validateRequest(request.getDeliveryAddressIndex() >= 0);
        RequestValidator.validateRequest(!request.getCombosMap().isEmpty() || !request.getDishesMap().isEmpty());
        validateCustomer(request);

        final long now = DateTime.now().getMillis();

        LOGGER.info("Load menu {}", request.getMenuId());
        Menu menu = menuDbClient.getById(request.getMenuId());
        if (menu == null) {
            throw new NotFoundException();
        }

        RequestValidator.validateNotNull(request.getDeliveryAddressIndex() < menu.getDeliveryAddressesCount());
        Address deliveryAddress = menu.getDeliveryAddresses(request.getDeliveryAddressIndex());

        Map<String, Dish> dishesById = new HashMap<>();
        Map<String, Price> dishPricesById = new HashMap<>();
        Map<String, Combo> combosById = new HashMap<>();
        Map<String, Price> comboPricesById = new HashMap<>();
        for (Menu.MenuEntry entry : menu.getMenuEntriesList()) {
            switch (entry.getEntryCase()) {
                case DISH:
                    dishesById.put(entry.getDish().getId(), entry.getDish());
                    dishPricesById.put(entry.getDish().getId(), entry.getPrice());
                    break;
                case COMBO:
                    combosById.put(entry.getCombo().getId(), entry.getCombo());
                    comboPricesById.put(entry.getCombo().getId(), entry.getPrice());
                    break;
                default:
                    throw new InternalRuntimeError("bad menu");
            }
        }

        Customer customer;
        if (StringUtils.isNotBlank(request.getCustomerId())) {
            LOGGER.info("Load customer {}", request.getCustomerId());
            customer = customerDbClient.getById(request.getCustomerId());
            if (customer == null) {
                throw new NotFoundException();
            }
        } else {
            customer = request.getCustomerObj()
                    .toBuilder()
                    .setId(CustomerDbClient.computeKey(request.getCustomerObj().getPhone(), request.getCustomerObj().getName()))
                    .clearAddresses()
                    .addAddresses(deliveryAddress)
                    .setCreationTime(now)
                    .build();
            LOGGER.info("Create customer {}", customer);
            customer = customerDbClient.putAndUpdateTimestamp(customer);
        }

        double totalPriceBeforeTax = 0d;
        List<Order.OrderEntry> orderEntries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : request.getDishesMap().entrySet()) {
            Dish dish = dishesById.get(entry.getKey());
            RequestValidator.validateNotNull(dish);
            RequestValidator.validateRequest(entry.getValue() > 0);
            Price price = dishPricesById.get(entry.getKey());
            totalPriceBeforeTax += price.getValue();
            orderEntries.add(Order.OrderEntry.newBuilder()
                    .setDish(dish)
                    .setAmount(entry.getValue())
                    .setUnitPrice(price)
                    .build());
        }
        for (Map.Entry<String, Integer> entry : request.getCombosMap().entrySet()) {
            Combo combo = combosById.get(entry.getKey());
            RequestValidator.validateNotNull(combo);
            RequestValidator.validateRequest(entry.getValue() > 0);
            Price price = comboPricesById.get(entry.getKey());
            totalPriceBeforeTax += price.getValue();
            orderEntries.add(Order.OrderEntry.newBuilder()
                    .setCombo(combo)
                    .setAmount(entry.getValue())
                    .setUnitPrice(price)
                    .build());
        }


        Order order = Order.newBuilder()
                .setStatus(INIT)
                .setCustomer(customer)
                .setDeliveryAddress(deliveryAddress)
                .addAllOrderEntries(orderEntries)
                .setTotalPriceBeforeTax(Price.newBuilder().setUnit(Price.Unit.USD).setValue(totalPriceBeforeTax))
                .setCreationTime(now)
                .setLastUpdatedTime(now)
                .addTags(menu.getId())
                .addTags(todayDate())
                .build();
        order = orderDbClient.put(order);
        LOGGER.info("Save order {}", order.getId());

        return Response.ok(order).build();
    }

    private static String todayDate() {
        return new LocalDateTime().toString("yyyy-MM-dd");
    }

    private static void validateCustomer(final InitOrderRequest request) {
        if (StringUtils.isBlank(request.getCustomerId())) {
            Customer customer = request.getCustomerObj();
            RequestValidator.validateNotBlank(customer.getName(), "customerName");
            RequestValidator.validateNotBlank(customer.getPhone().getPhone(), "customerPhone");
        }
    }
}
