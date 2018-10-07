package jiaoni.daigou.tools;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.DatastoreDbClient;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DatastoreEntityFactory;
import jiaoni.common.appengine.access.db.DbClient;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.impls.CustomerDbClient;
import jiaoni.daigou.tools.remote.RemoteApi;
import jiaoni.daigou.wiremodel.entity.Address;
import jiaoni.daigou.wiremodel.entity.Customer;
import jiaoni.daigou.wiremodel.entity.PhoneNumber;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.UUID;

import static com.google.api.client.util.Preconditions.checkState;

/**
 * Verify {@link DbClient}.
 */
public class VerifyDbClient {
    public static void main(String[] args) throws Exception {
        Customer customer = Customer.newBuilder()
                .setId("coolkey")
                .setName("test")
                .setPhone(PhoneNumber.newBuilder().setPhone("12345678901").setCountryCode("86").build())
                .addAddresses(Address.newBuilder().setRegion("r1").build())
                .addAddresses(Address.newBuilder().setRegion("r2").build())
                .build();
        try (RemoteApi remoteApi = RemoteApi.login()) {
            CustomerDbClient dbClient = new CustomerDbClient(
                    remoteApi.getDatastoreService(),
                    remoteApi.getMemcacheService(),
                    Env.DEV);
            dbClient.put(customer);
        }
    }

    private static void testDummp() throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            ItemDbClient client = new ItemDbClient(remoteApi.getDatastoreService());

            String name = UUID.randomUUID().toString();

            // Create item
            Item item = new Item();
            item.name = name;

            Item savedItem = client.put(item);
            System.out.println("savedItem: " + savedItem);
            checkState(savedItem.id != null);
            checkState(name.equals(savedItem.name));

            String id = savedItem.id;

            // Get by id
            Item fetchedItem = client.getById(id);
            checkState(fetchedItem.equals(savedItem));
        }
    }

    private static class ItemDbClient extends DatastoreDbClient<Item> {
        ItemDbClient(DatastoreService service) {
            super(service, new ItemFactory());
        }
    }

    private static class Item {
        private String id;
        private String name;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

        @Override
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private static class ItemFactory implements DatastoreEntityFactory<Item> {
        private static final String FIELD_NAME = "item";

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public String getKind() {
            return "suibian";
        }

        @Override
        public Item fromEntity(DatastoreEntityExtractor entity) {
            Item item = new Item();
            item.id = entity.getKeyLongId();
            item.name = entity.getAsString(FIELD_NAME);
            return item;
        }

        @Override
        public String getId(Item obj) {
            return obj.id;
        }

        @Override
        public Item mergeId(Item obj, String id) {
            obj.id = id;
            return obj;
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Item obj) {
            return partialBuilder.unindexedString(FIELD_NAME, obj.name)
                    .build();
        }
    }
}
