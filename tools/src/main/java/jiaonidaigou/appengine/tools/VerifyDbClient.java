package jiaonidaigou.appengine.tools;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaonidaigou.appengine.api.access.db.core.DatastoreClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DbClient;
import jiaonidaigou.appengine.tools.remote.RemoteApi;
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

    private static class ItemDbClient extends DatastoreClient<Item> {
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
            return "VerifyDbClient";
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
