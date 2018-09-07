package jiaonidaigou.appengine.lib.teddy;

import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import jiaonidaigou.appengine.lib.teddy.model.Admin;
import org.junit.Test;

public class TestEE {
    //    JIAO("1437",
//                 "xiaoxiao9143@gmail.com",
//                 "JAY2020405630",
//                 "娇妮",
//                 "2138801085",
//                 "sammamish"),
//    HACK("1899",
//                 "songfan.rfu@gmail.com",
//                 "furuijie",
//                 "Song Fan",
//                 "2137171237",
//                 "Seattle");
    @Test
    public void tt() throws Exception {
        Admin admin = Admin.builder()
                .withUserId("1437")
                .withLoginUsername("xiaoxiao9143@gmail.com")
                .withLoginPassword("JAY2020405630")
                .withSenderName("娇妮")
                .withSenderPhone("2138801085")
                .withSenderAddress("sammamish")
                .build();

        Admin admin2 = Admin.builder()
                .withUserId("1899")
                .withLoginUsername("songfan.rfu@gmail.com")
                .withLoginPassword("furuijie")
                .withSenderName("Song Fan")
                .withSenderPhone("2137171237")
                .withSenderAddress("Seattle")
                .build();

        String json = ObjectMapperProvider.get()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(admin2);
        System.out.println(json);
    }
}
