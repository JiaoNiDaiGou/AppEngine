package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import jiaonidaigou.appengine.api.LocalServiceRule;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DatastoreDbClientLongIdTest {

    @Rule
    public final LocalServiceRule localService = LocalServiceRule
            .builder()
            .useDatastore()
            .build();

    private ItemDbClient underTest;

    @Before
    public void setUp() {
        underTest = new ItemDbClient(localService.getDatastoreService());
    }

    @Test
    public void test_put_get_same() {
        Item item = new Item("name");
        Item afterSave = underTest.put(item);
        assertNotNull(afterSave.id);
        Item fetched = underTest.getById(afterSave.id);
        assertEquals(afterSave, fetched);
        Item afterSaveAgain = underTest.put(fetched);
        assertEquals(afterSave, afterSaveAgain);
        assertEquals(1L, underTest.scan().count());
    }

    @Test
    public void test_put_get() {
        List<Item> items = underTest.put(
                new Item("name1"),
                new Item("name2"),
                new Item("name3")
        );
        List<Item> fetchedItems = items.stream()
                .map(t -> t.id)
                .peek(t -> System.out.println(t))
                .map(t -> underTest.getById(t))
                .collect(Collectors.toList());
        assertEquals(items, fetchedItems);
    }

    @Test
    public void test_put_get_delete() {
        Item item = underTest.put(new Item("1"));

        Item fetchedItem = underTest.getById(item.id);
        assertEquals(item, fetchedItem);

        underTest.delete(item.id);
        fetchedItem = underTest.getById(item.id);
        assertNull(fetchedItem);
    }

    @Test
    public void test_scan() {
        List<Item> items = underTest.put(
                new Item("name1"),
                new Item("name2"),
                new Item("name3")
        );

        List<Item> fetchedItems = underTest.scan()
                .sorted()
                .collect(Collectors.toList());
        assertEquals(items, fetchedItems);
    }

    @Test
    public void test_keyRange() {
        underTest.put(
                new Item("name1"),
                new Item("name2"),
                new Item("name3"),
                new Item("name4"),
                new Item("name5")
        );

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
        underTest.put(
                new Item("name1"),
                new Item("name2"),
                new Item("name3"),
                new Item("name4"),
                new Item("name5")
        );

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

    @Test
    public void testBooleanIndexQuery() {
        Item a = new Item("namea");
        a.booleanField = true;
        Item b = new Item("nameb");
        b.booleanField = false;
        underTest.put(a, b);
        List<Item> items = underTest.queryInStream(DbQuery.eq(ItemFactory.FIELD_BOOLEAN, true)).collect(Collectors.toList());
        assertEquals(Collections.singletonList(a), items);
    }

    private static void assertIdsMatch(Iterable<Item> actualItems, String... expectedIds) {
        List<String> expected = Arrays.stream(expectedIds).sorted().collect(Collectors.toList());
        List<String> actual = Streams.stream(actualItems).sorted().map(t -> t.id).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    private static class ItemDbClient extends DatastoreDbClient<Item> {
        ItemDbClient(DatastoreService service) {
            super(service, new ItemFactory());
        }
    }

    private static class Item implements Comparable<Item> {
        private String id;
        private String name;
        private boolean booleanField;

        Item(String name) {
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
        private static final String FIELD_BOOLEAN = "field_boolean";

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public String getKind() {
            return "anything_works";
        }

        @Override
        public Item fromEntity(DatastoreEntityExtractor entity) {
            Item item = new Item(entity.getAsString(FIELD_NAME));
            item.id = entity.getKeyLongId();
            item.booleanField = entity.getAsBoolean(FIELD_BOOLEAN);
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
            return partialBuilder
                    .indexedString(FIELD_NAME, obj.name)
                    .indexedBoolean(FIELD_BOOLEAN, obj.booleanField)
                    .build();
        }
    }
}
