package jiaoni.songfan.service.integrationtest;

import jiaoni.common.appengine.access.db.DoNothingMemcache;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.ApiClient;
import jiaoni.common.test.RemoteApi;
import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.PhoneNumber;
import jiaoni.common.wiremodel.Price;
import jiaoni.songfan.service.appengine.AppEnvs;
import jiaoni.songfan.service.appengine.impls.CustomerDbClient;
import jiaoni.songfan.service.appengine.impls.DishDbClient;
import jiaoni.songfan.service.appengine.impls.MenuDbClient;
import jiaoni.songfan.wiremodel.api.InitOrderRequest;
import jiaoni.songfan.wiremodel.entity.Customer;
import jiaoni.songfan.wiremodel.entity.Dish;
import jiaoni.songfan.wiremodel.entity.Menu;
import jiaoni.songfan.wiremodel.entity.Order;
import org.junit.Test;

import java.util.UUID;
import javax.ws.rs.client.Entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OrderIntegrationTest {
    private final ApiClient apiClient = new ApiClient(AppEnvs.getHostname(Env.DEV));

    @Test
    public void testInitOrder_knownCustomer() throws Exception {
        // Input a customer
        Customer customer = Customer.newBuilder()
                .setId("customer_id_" + UUID.randomUUID().toString())
                .setName("tester")
                .setPhone(PhoneNumber.newBuilder().setCountryCode("1").setPhone("1234567890"))
                .build();

        Dish dish;
        Menu menu;
        try (RemoteApi remoteApi = RemoteApi.login()) {
            CustomerDbClient customerDbClient = new CustomerDbClient(Env.DEV, remoteApi.getDatastoreService());
            customer = customerDbClient.putAndUpdateTimestamp(customer);

            DishDbClient dishDbClient = new DishDbClient(Env.DEV, remoteApi.getDatastoreService());
            dish = dishDbClient.put(Dish.newBuilder().setName("Fish").build());

            menu = Menu.newBuilder()
                    .setId("menu_id_" + UUID.randomUUID().toString())
                    .addDeliveryAddresses(Address.newBuilder().setAddress("add"))
                    .addMenuEntries(
                            Menu.MenuEntry.newBuilder()
                                    .setDish(dish)
                                    .setPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(10))
                    )
                    .setCreationTime(System.currentTimeMillis())
                    .build();
            MenuDbClient menuDbClient = new MenuDbClient(Env.DEV, remoteApi.getDatastoreService(), new DoNothingMemcache());
            menu = menuDbClient.put(menu);
        }

        // Get dish
        Dish fetchedDish = apiClient.newTarget()
                .path("/api/dishes/" + dish.getId())
                .request()
                .get()
                .readEntity(Dish.class);
        assertNotNull(fetchedDish);

        // Get menu
        Menu fetchedMenu = apiClient.newTarget()
                .path("/api/menus/" + menu.getId())
                .request()
                .get()
                .readEntity(Menu.class);
        assertNotNull(fetchedMenu);

        System.out.println(ObjectMapperProvider.prettyToJson(fetchedMenu));

        // Make order
        InitOrderRequest request = InitOrderRequest.newBuilder()
                .setCustomerId(customer.getId())
                .setDeliveryAddressIndex(0)
                .setMenuId(menu.getId())
                .putDishes(dish.getId(), 2)
                .build();
        Order order = apiClient.newTarget()
                .path("/api/orders/init")
                .request()
                .post(Entity.json(request))
                .readEntity(Order.class);
        System.out.println(ObjectMapperProvider.compactToJson(order));

        Order fetchedOrder = apiClient.newTarget()
                .path("/api/orders/" + order.getId())
                .request()
                .get()
                .readEntity(Order.class);
        assertEquals(order, fetchedOrder);
    }

    @Test
    public void testInitOrder_uknownCustomer() throws Exception {
        Dish dish;
        Menu menu;
        try (RemoteApi remoteApi = RemoteApi.login()) {
            DishDbClient dishDbClient = new DishDbClient(Env.DEV, remoteApi.getDatastoreService());
            dish = dishDbClient.put(Dish.newBuilder().setName("Fish").build());

            menu = Menu.newBuilder()
                    .setId("menu_id_" + UUID.randomUUID().toString())
                    .addDeliveryAddresses(Address.newBuilder().setAddress("add"))
                    .addMenuEntries(
                            Menu.MenuEntry.newBuilder()
                                    .setDish(dish)
                                    .setPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(10))
                    )
                    .setCreationTime(System.currentTimeMillis())
                    .build();
            MenuDbClient menuDbClient = new MenuDbClient(Env.DEV, remoteApi.getDatastoreService(), new DoNothingMemcache());
            menu = menuDbClient.put(menu);
        }

        // Get dish
        Dish fetchedDish = apiClient.newTarget()
                .path("/api/dishes/" + dish.getId())
                .request()
                .get()
                .readEntity(Dish.class);
        assertNotNull(fetchedDish);

        // Get menu
        Menu fetchedMenu = apiClient.newTarget()
                .path("/api/menus/" + menu.getId())
                .request()
                .get()
                .readEntity(Menu.class);
        assertNotNull(fetchedMenu);

        // Make order
        Customer customer = Customer.newBuilder()
                .setName("customer_name_" + UUID.randomUUID().toString())
                .setPhone(PhoneNumber.newBuilder().setCountryCode("1").setPhone("1234567890"))
                .build();
        InitOrderRequest request = InitOrderRequest.newBuilder()
                .setCustomerObj(customer)
                .setDeliveryAddressIndex(0)
                .setMenuId(menu.getId())
                .putDishes(dish.getId(), 2)
                .build();
        Order order = apiClient.newTarget()
                .path("/api/orders/init")
                .request()
                .post(Entity.json(request))
                .readEntity(Order.class);
        System.out.println(ObjectMapperProvider.compactToJson(order));

        Order fetchedOrder = apiClient.newTarget()
                .path("/api/orders/" + order.getId())
                .request()
                .get()
                .readEntity(Order.class);
        assertEquals(order, fetchedOrder);
    }
}
