package jiaoni.songfan.service.appengine.interfaces;

import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.Price;
import jiaoni.songfan.service.appengine.impls.MenuDbClient;
import jiaoni.songfan.wiremodel.entity.Dish;
import jiaoni.songfan.wiremodel.entity.Menu;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/menus")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Singleton
public class MenuInterface {
    private final MenuDbClient dbClient;

    @Inject
    public MenuInterface(final MenuDbClient dbClient) {
        this.dbClient = dbClient;
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") final String id) {
//        Menu menu = dbClient.getById(id);
//        if (menu == null) {
//            throw new NotFoundException();
//        }
//        return Response.ok(menu).build();

        // Return hardcoded menu
        Dish dishFish = Dish.newBuilder()
                .setId("dish_fish")
                .setName("西湖醋鱼")
                .setDescription("西湖醋鱼（West Lake Fish in Vinegar Gravy），别名为叔嫂传珍，宋嫂鱼，是浙江杭州饭店的一道传统地方风味名菜")
                .addMediaIds("dish_fish_a.jpg")
                .addMediaIds("dish_fish_b.jpg")
                .addMediaIds("dish_fish_c.jpg")
                .build();

        Dish dishChicken = Dish.newBuilder()
                .setId("dish_chicken")
                .setName("香酥鸡")
                .setDescription("香酥鸡是山东地区特色传统风味名菜之一，属于鲁菜菜系。传遍济南、青岛、烟台等地区。此菜选用笋母鸡，以高汤蒸熟，火候足到，入油再炸，焦酥异常，其色红润，肉烂味美，是佐酒之美味。")
                .addMediaIds("dish_chicken_a.jpg")
                .addMediaIds("dish_checken_b.jpg")
                .build();

        Dish dishMeat = Dish.newBuilder()
                .setId("dish_meat")
                .setName("东坡肉")
                .setDescription("东坡肉，又名滚肉、东坡焖肉，是眉山和江南地区特色传统名菜。东坡肉在浙菜、川菜、鄂菜等菜系中都有，且各地做法也有不同，有先煮后烧的，有先煮后蒸的，有直接焖煮收汁的。")
                .addMediaIds("dish_meat_a.jpg")
                .addMediaIds("dish_meat_b.jpg")
                .addMediaIds("dish_meat_c.jpg")
                .addMediaIds("dish_meat_d.jpg")
                .build();

        Address addressAmazon = Address.newBuilder()
                .setName("亚马逊 Fiona")
                .setRegion("WA")
                .setCity("Seattle")
                .setAddress("500 Boren Ave N (at Mercer St)")
                .setPostalCode("98019")
                .build();

        Address addressSnap = Address.newBuilder()
                .setName("Snap")
                .setRegion("WA")
                .setCity("Seattle")
                .setAddress("101 Stewart St")
                .setPostalCode("98101")
                .build();

        Menu menu = Menu.newBuilder()
                .setId("menu_id")
                .addMenuEntries(Menu.MenuEntry.newBuilder()
                        .setDish(dishFish)
                        .setPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(10)))
                .addMenuEntries(Menu.MenuEntry.newBuilder()
                        .setDish(dishChicken)
                        .setPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(15)))
                .addMenuEntries(Menu.MenuEntry.newBuilder()
                        .setDish(dishMeat)
                        .setPrice(Price.newBuilder().setUnit(Price.Unit.USD).setValue(18)))
                .setCreationTime(System.currentTimeMillis())
                .addDeliveryAddresses(addressAmazon)
                .addDeliveryAddresses(addressSnap)
                .setExpirationTime(Long.MAX_VALUE)
                .build();

        return Response.ok(menu).build();
    }
}
