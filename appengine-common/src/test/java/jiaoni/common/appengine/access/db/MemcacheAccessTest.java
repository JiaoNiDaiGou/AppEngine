package jiaoni.common.appengine.access.db;

import com.google.appengine.api.memcache.Expiration;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import jiaoni.common.appengine.access.LocalServiceRule;
import jiaoni.common.model.ProtoBytesBiTransform;
import jiaoni.daigou.wiremodel.entity.Customer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MemcacheAccessTest {
    private static final String NS = "ns";
    private static Expiration EXP = Expiration.byDeltaSeconds(100);

    @Rule
    public final LocalServiceRule localService = LocalServiceRule
            .builder()
            .useMemcache()
            .build();

    private MemcacheAccess<Customer> underTest;

    @Before
    public void setUp() {
        underTest = new MemcacheAccess<>(localService.memcacheService(), NS, new ProtoBytesBiTransform<>(Customer.parser()), EXP);
    }

    @Test
    public void testPut_get_delete() {
        Customer customer = customer();
        underTest.put("key", customer);

        assertEquals(customer, underTest.get("key"));

        underTest.delete("key");
        assertNull(underTest.get("key"));
    }

    @Test
    public void testPutAll_getAll_deletAll() {
        Map<String, Customer> map = ImmutableMap.copyOf(customers(10).stream().collect(Collectors.toMap(Customer::getName, t -> t)));
        underTest.putAll(map);

        assertEquals(map, underTest.getAll(map.keySet()));

        assertEquals(map, map.keySet().stream().map(t -> underTest.get(t)).collect(Collectors.toMap(Customer::getName, t -> t)));

        String deleteKey = map.keySet().iterator().next();
        underTest.delete(deleteKey);

        Map<String, Customer> afterDelete = new HashMap<>(map);
        afterDelete.remove(deleteKey);
        assertEquals(afterDelete, underTest.getAll(map.keySet()));

        underTest.delete(map.keySet());
        assertTrue(underTest.getAll(map.keySet()).isEmpty());
    }

    @Test
    public void testPutCollection_getCollection() {
        List<Customer> customers = customers(20);
        underTest.putCollection("key", customers);

        assertEquals(customers, underTest.getCollection("key"));

        underTest.delete("key");

        assertTrue(underTest.getCollection("key").isEmpty());
    }

    @Test
    public void testPutAllCollection_getAllCollection() {
        Multimap<String, Customer> customers = ArrayListMultimap.create();
        customers.putAll("keya", customers(10));
        customers.putAll("keyb", customers(20));

        underTest.putAllCollection(customers);

        assertEquals(customers.get("keya"), underTest.getCollection("keya"));
        assertEquals(customers.get("keyb"), underTest.getCollection("keyb"));

        underTest.delete("keya");
        assertTrue(underTest.getCollection("keya").isEmpty());
    }

    @Test
    public void testPutShards_getShards_deleteShards() {
        List<String> shards = Arrays.asList("a", "b", "c");
        underTest.putShards(shards);

        assertEquals(shards, underTest.getShards());

        underTest.deleteShards();

        assertTrue(underTest.getShards().isEmpty());
    }

    @Test
    public void testPutAllWithShards_getAllWithShards_deleteShards() {
        List<Customer> customers = customers(1000);

        int partitionSize = 50;
        underTest.putAllWithShards(customers, partitionSize);

        assertEquals(customers.size() / partitionSize, underTest.getShards().size());

        assertEquals(customers, underTest.getAllWithShards());

        underTest.deleteShards();

        assertTrue(underTest.getAllWithShards().isEmpty());
    }

    private static Customer customer() {
        return Customer.newBuilder().setName("name-" + UUID.randomUUID().toString()).build();
    }

    private static List<Customer> customers(int size) {
        return IntStream.range(0, size).mapToObj(t -> customer()).collect(Collectors.toList());
    }
}
