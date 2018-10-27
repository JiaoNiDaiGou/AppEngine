package jiaoni.common.appengine.access.db;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.Expiration;
import jiaoni.common.appengine.access.LocalServiceRule;
import jiaoni.common.model.Env;
import jiaoni.common.model.ProtoBytesBiTransform;
import jiaoni.daigou.wiremodel.entity.Customer;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MemcacheDbClientTest {
    private static final String NS = "ns";
    private static Expiration EXP = Expiration.byDeltaSeconds(100);

    @Rule
    public final LocalServiceRule localService = LocalServiceRule
            .builder()
            .useMemcache()
            .useDatastore()
            .build();

    private MemcacheDbClient<Customer> underTest;

    private DatastoreDbClient<Customer> dbClient;

    @Before
    public void setUp() {
        dbClient = Mockito.spy(new DatastoreDbClient<>(localService.getDatastoreService(), new TestCustomerEntityFactory()));
        underTest = new MemcacheDbClient<>(
                NS,
                localService.memcacheService(),
                dbClient,
                Customer::getId,
                new ProtoBytesBiTransform<>(Customer.parser()),
                true);
    }

    @Test
    public void testQueryInPagination() {
        List<Customer> customers = underTest.put(customers(1000))
                .stream()
                .sorted(Comparator.comparing(Customer::getId))
                .collect(Collectors.toList());

        underTest.scan().collect(Collectors.toList()); // Warm up memcache
        reset(dbClient);

        Predicate<Customer> predicate = t -> t.getName().endsWith("9");
        DbQuery dbQuery = DbQuery.inMemory(predicate);
        int limit = 10;
        List<Customer> expectedResults = customers.stream().filter(predicate).collect(Collectors.toList());

        PaginatedResults<Customer> firstPage = underTest.queryInPagination(dbQuery, limit, null);
        assertNotNull(firstPage.getPageToken());
        assertEquals(expectedResults.size(), firstPage.getTotoalCount().intValue());
        assertEquals(expectedResults.subList(0, limit), firstPage.getResults());

        PaginatedResults<Customer> secondPage = underTest.queryInPagination(dbQuery, limit, PageToken.fromPageToken(firstPage.getPageToken()));
        assertNotNull(secondPage.getPageToken());
        assertEquals(expectedResults.size(), secondPage.getTotoalCount().intValue());
        assertEquals(expectedResults.subList(limit, 2 * limit), secondPage.getResults());

        // All return from cache
        verifyZeroInteractions(dbClient);
    }

    private static Customer customer(int nameIndex) {
        return Customer.newBuilder().setName("name-" + nameIndex).build();
    }

    private static List<Customer> customers(int size) {
        return IntStream.range(0, size).mapToObj(MemcacheDbClientTest::customer).collect(Collectors.toList());
    }

    private static class TestCustomerEntityFactory extends BaseEntityFactory<Customer> {
        static final String FIELD_NAME = "nam";

        protected TestCustomerEntityFactory() {
            super(Env.DEV);
        }

        @Override
        protected String getServiceName() {
            return "svc";
        }

        @Override
        protected String getTableName() {
            return "tab";
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public Customer fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf("dat", Customer.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Customer obj) {
            return partialBuilder
                    .indexedString(FIELD_NAME, obj.getName())
                    .unindexedProto("dat", obj)
                    .build();
        }

        @Override
        public Customer mergeId(Customer obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(Customer obj) {
            return obj.getId();
        }
    }
}
