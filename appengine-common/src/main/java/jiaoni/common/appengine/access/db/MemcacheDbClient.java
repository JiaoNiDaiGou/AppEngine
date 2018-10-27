package jiaoni.common.appengine.access.db;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.collect.ImmutableMap;
import jiaoni.common.appengine.access.db.DbQuery.InMemoryQuery;
import jiaoni.common.model.BiTransform;
import jiaoni.common.utils.Preconditions2;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A DBClient implementation with Memcache support.
 * <p>
 * NOTE for query:
 * if the query contains non-{@link InMemoryQuery}, will skip Memcache even if cacheAll is enabled.
 * In other word, if cacheAll is enabled, suggest only issue a pure {@link InMemoryQuery}.
 *
 * @param <T> Type of entity.
 */
public class MemcacheDbClient<T> implements DbClient<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemcacheDbClient.class);

    private static final Expiration DEFAULT_EXPIRATION = Expiration.byDeltaSeconds(4 * 60 * 60); //4h

    private static final int DEFAULT_PARTITION_SIZE = 200;

    private final DbClient<T> dbClient;
    private final IdGetter<T> idGetter;
    private final boolean cacheAll;
    private final MemcacheAccess<T> memcache;

    public MemcacheDbClient(final String namespace,
                            final MemcacheService memcache,
                            final DbClient<T> dbClient,
                            final IdGetter<T> idGetter,
                            final BiTransform<T, byte[]> memcacheTransform,
                            final boolean cacheAll) {
        this.dbClient = checkNotNull(dbClient);
        this.idGetter = checkNotNull(idGetter);
        this.cacheAll = cacheAll;
        this.memcache = new MemcacheAccess<>(
                checkNotNull(memcache),
                Preconditions2.checkNotBlank(namespace),
                checkNotNull(memcacheTransform),
                DEFAULT_EXPIRATION);
    }

    @Override
    public T put(T obj) {
        T afterPut = dbClient.put(obj);
        memcache.put(idGetter.getId(afterPut), afterPut);
        if (cacheAll) {
            memcache.deleteShards();
        }
        return afterPut;
    }

    @Override
    public List<T> put(T... objs) {
        return put(Arrays.asList(objs));
    }

    @Override
    public List<T> put(List<T> objs) {
        List<T> afterPut = dbClient.put(objs);

        Map<String, T> toPutMemcache = new HashMap<>();
        objs.forEach(t -> toPutMemcache.put(idGetter.getId(t), t));
        memcache.putAll(toPutMemcache);
        if (cacheAll) {
            memcache.deleteShards();
        }
        return afterPut;
    }

    @Override
    public T getById(String id) {
        T fromCache = memcache.get(id);
        if (fromCache != null) {
            return fromCache;
        }
        T fromDb = dbClient.getById(id);
        if (fromDb == null) {
            return null;
        }
        memcache.put(idGetter.getId(fromDb), fromDb);
        if (cacheAll) {
            memcache.deleteShards();
        }
        return fromDb;
    }

    @Override
    public Map<String, T> getByIds(List<String> ids) {
        Map<String, T> fromCache = memcache.getAll(ids);
        List<String> toLoadFromDd = new ArrayList<>(CollectionUtils.subtract(ids, fromCache.keySet()));
        Map<String, T> fromDb = ImmutableMap.of();
        if (!toLoadFromDd.isEmpty()) {
            fromDb = dbClient.getByIds(toLoadFromDd);
            memcache.putAll(fromDb);
            if (cacheAll) {
                memcache.deleteShards();
            }
        }
        return ImmutableMap.<String, T>builder()
                .putAll(fromCache)
                .putAll(fromDb)
                .build();
    }

    @Override
    public void delete(String id) {
        dbClient.delete(id);
        memcache.delete(id);
        if (cacheAll) {
            memcache.deleteShards();
        }
    }

    @Override
    public void delete(String... ids) {
        delete(Arrays.asList(ids));
    }

    @Override
    public void delete(List<String> ids) {
        dbClient.delete(ids);
        memcache.delete(ids);
        if (cacheAll) {
            memcache.deleteShards();
        }
    }

    @Override
    public void deleteItem(T obj) {
        dbClient.deleteItem(obj);
        memcache.delete(idGetter.getId(obj));
        if (cacheAll) {
            memcache.deleteShards();
        }
    }

    @Override
    public void deleteItems(List<T> objs) {
        dbClient.deleteItems(objs);
        memcache.delete(objs.stream()
                .map(idGetter::getId)
                .collect(Collectors.toList()));
        if (cacheAll) {
            memcache.deleteShards();
        }
    }

    @Override
    public Stream<T> scan() {
        if (!cacheAll) {
            return dbClient.scan();
        }

        List<T> fromCache = memcache.getAllWithShards();

        if (CollectionUtils.isNotEmpty(fromCache)) {
            LOGGER.info("Scan results from Memcache");
            return fromCache.stream();
        }

        LOGGER.info("Scan results from DB");
        List<T> fromDb = dbClient.scan().collect(Collectors.toList());

        memcache.putAllWithShards(fromDb, DEFAULT_PARTITION_SIZE);

        return fromDb.stream();
    }

    @Override
    public Stream<T> queryInStream(DbQuery query) {
        if (query == null) {
            return scan();
        }

        return canQueryFromMemcache(query)
                ? scan().filter(((InMemoryQuery<T>) query).getPredicate()).sorted(Comparator.comparing(idGetter::getId))
                : dbClient.queryInStream(query);
    }

    @Override
    public PaginatedResults<T> queryInPagination(DbQuery query, int limit, PageToken pageToken) {
        boolean canQueryFromMemcache = canQueryFromMemcache(query);
        if (pageToken != null) {
            if (pageToken.isSourceInMemory() && !canQueryFromMemcache) {
                throw new IllegalArgumentException("Do not support this. The page token is in-memory. but the query doesn't support in-memory.");
            }
        }
        if (!canQueryFromMemcache) {
            return dbClient.queryInPagination(query, limit, pageToken);
        }

        List<T> results = scan()
                .filter(((InMemoryQuery<T>) query).getPredicate())
                .sorted(Comparator.comparing(idGetter::getId))
                .collect(Collectors.toList());
        int totalSize = results.size();
        int startIndex = pageToken == null ? 0 : pageToken.getIndex();
        int endIndex = Math.min(startIndex + limit, results.size());
        boolean hasNextPageToken = endIndex != results.size();
        if (startIndex != 0 || hasNextPageToken) {
            results = results.subList(startIndex, endIndex);
        }
        return PaginatedResults.<T>builder()
                .withPageToken(hasNextPageToken ? PageToken.inMemory(endIndex).toPageToken() : null)
                .withResults(results)
                .withTotoalCount(totalSize)
                .build();
    }

    private boolean canQueryFromMemcache(DbQuery dbQuery) {
        if (!cacheAll || !(dbQuery instanceof InMemoryQuery)) {
            return false;
        }
        InMemoryQuery inMemoryQuery = (InMemoryQuery) dbQuery;
        return !inMemoryQuery.hasDbQuery();
    }
}
