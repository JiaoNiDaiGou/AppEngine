package jiaoni.common.appengine.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import jiaoni.common.appengine.access.LocalServiceRule;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DatastoreDbClientStringNameTest {

    @Rule
    public final LocalServiceRule localService = LocalServiceRule
            .builder()
            .useDatastore()
            .build();

    private ItemDbClient underTest;

    private static void assertIdsMatch(Iterable<Item> actualItems, String... expectedIds) {
        List<String> expected = Arrays.stream(expectedIds).sorted().collect(Collectors.toList());
        List<String> actual = Streams.stream(actualItems).sorted().map(t -> t.id).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Before
    public void setUp() {
        underTest = new ItemDbClient(localService.getDatastoreService());
    }

    @Test
    public void test_put_get() {
        List<Item> items = Arrays.asList(
                new Item("1"),
                new Item("2"),
                new Item("3")
        );
        underTest.put(items);
        List<Item> fetchedItems = Arrays.asList(
                underTest.getById("1"),
                underTest.getById("2"),
                underTest.getById("3")
        );
        assertEquals(items, fetchedItems);
    }

    @Test
    public void test_put_get_delete() {
        Item fetchedItem = underTest.getById("1");
        assertNull(fetchedItem);

        Item item = new Item("1");
        underTest.put(item);

        fetchedItem = underTest.getById("1");
        assertEquals(item, fetchedItem);

        underTest.delete("1");
        fetchedItem = underTest.getById("1");
        assertNull(fetchedItem);
    }

    @Test
    public void test_scan() {
        List<Item> items = Arrays.asList(
                new Item("1", "name1"),
                new Item("2", "name2"),
                new Item("3", "name3")
        );
        underTest.put(items);

        List<Item> fetchedItems = underTest.scan()
                .sorted()
                .collect(Collectors.toList());
        assertEquals(items, fetchedItems);
    }

    @Test
    public void test_keyRange() {
        List<Item> items = Arrays.asList(
                new Item("1", "name1"),
                new Item("2", "name2"),
                new Item("3", "name3"),
                new Item("4", "name4"),
                new Item("5", "name5")
        );
        underTest.put(items);

        // GE
        List<Item> fetchedItems = underTest.queryInStream(
                DbQuery.keyRange(Range.atLeast("3")))
                .collect(Collectors.toList());
        assertIdsMatch(fetchedItems, "3", "4", "5");

        // GT
        fetchedItems = underTest.queryInStream(
                DbQuery.keyRange(Range.greaterThan("3")))
                .collect(Collectors.toList());
        assertIdsMatch(fetchedItems, "4", "5");

        // GT & LE
        fetchedItems = underTest.queryInStream(
                DbQuery.keyRange(Range.openClosed("2", "4"))
        ).collect(Collectors.toList());
        assertIdsMatch(fetchedItems, "3", "4");
    }

    @Test
    public void test_query() {
        List<Item> items = Arrays.asList(
                new Item("1", "name1"),
                new Item("2", "name2"),
                new Item("3", "name3"),
                new Item("4", "name4"),
                new Item("5", "name5")
        );
        underTest.put(items);

        // GE
        List<Item> fetchedItems = underTest.queryInStream(
                DbQuery.ge(ItemFactory.FIELD_NAME, "name2")
        ).collect(Collectors.toList());
        assertIdsMatch(fetchedItems, "2", "3", "4", "5");

        // GT & LE
        fetchedItems = underTest.queryInStream(
                DbQuery.and(DbQuery.gt(ItemFactory.FIELD_NAME, "name2"), DbQuery.le(ItemFactory.FIELD_NAME, "name4"))
        ).collect(Collectors.toList());
        assertIdsMatch(fetchedItems, "3", "4");

        // GT | LE
        fetchedItems = underTest.queryInStream(
                DbQuery.or(DbQuery.gt(ItemFactory.FIELD_NAME, "name4"), DbQuery.le(ItemFactory.FIELD_NAME, "name2"))
        ).collect(Collectors.toList());
        assertIdsMatch(fetchedItems, "1", "2", "5");
    }

    private static class ItemDbClient extends DatastoreDbClient<Item> {
        ItemDbClient(DatastoreService service) {
            super(service, new ItemFactory());
        }
    }

    private static class Item implements Comparable<Item> {
        private String id;
        private String name;

        Item(String id) {
            this(id, UUID.randomUUID().toString());
        }

        Item(String id, String name) {
            this.id = id;
            this.name = name;
        }

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

        @Override
        public int compareTo(Item o) {
            return this.id.compareTo(o.id);
        }
    }

    private static class ItemFactory implements DatastoreEntityFactory<Item> {
        private static final String FIELD_NAME = "item";

        @Override
        public KeyType getKeyType() {
            return KeyType.STRING_NAME;
        }

        @Override
        public String getKind() {
            return "anything_works";
        }

        @Override
        public Item fromEntity(DatastoreEntityExtractor entity) {
            return new Item(entity.getKeyStringName(), entity.getAsString(FIELD_NAME));
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
            return partialBuilder.indexedString(FIELD_NAME, obj.name).build();
        }
    }
}
