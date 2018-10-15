package jiaoni.common.appengine.access.db;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import jiaoni.common.model.BiTransform;
import jiaoni.common.wiremodel.BytesArray;
import jiaoni.common.wiremodel.StringArray;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A DBClient implementation based on Memcache.
 * NOTE we currently only cache get/put by ID.
 * query is not supported.
 *
 * @param <T> Type of entity.
 */
public class MemcacheDbClient<T> implements DbClient<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemcacheDbClient.class);

    private static final Expiration DEFAULT_EXPIRATION = Expiration.byDeltaSeconds(4 * 60 * 60); //4h

    private static final int DEFAULT_PARTITION_SIZE = 200;

    private final String namespace;
    private final MemcacheService memcache;
    private final DbClient<T> dbClient;
    private final IdGetter<T> idGetter;
    private final BiTransform<T, byte[]> memcacheTransform;
    private final boolean cacheCall;

    public MemcacheDbClient(final String namespace,
                            final MemcacheService memcache,
                            final DbClient<T> dbClient,
                            final IdGetter<T> idGetter,
                            final BiTransform<T, byte[]> memcacheTransform,
                            final boolean cacheAll) {
        this.namespace = checkNotNull(namespace);
        this.memcache = checkNotNull(memcache);
        this.dbClient = checkNotNull(dbClient);
        this.idGetter = checkNotNull(idGetter);
        this.memcacheTransform = checkNotNull(memcacheTransform);
        this.cacheCall = cacheAll;
    }

    private String withNamespace(String id) {
        return namespace + "." + id;
    }

    /**
     * We load all items from DB, split them into several partitions, and then put them into different
     * Memcache blobs. And we also store these keys into a separate Memcache blob. So, once scan() is called,
     * we fisrt check this blob to get all partitions keys. Then load all partitions.
     */
    private String allCacheKey() {
        return namespace + ".@ALL";
    }

    private <M> M loadCache(final String key, final Function<byte[], M> transform) {
        byte[] bytes = (byte[]) memcache.get(key);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return transform.apply(bytes);
    }

    private <M> Map<String, M> loadCache(final List<String> keys, final Function<byte[], M> transfrom) {
        Map<String, Object> map = memcache.getAll(keys);
        Map<String, M> toReturn = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            byte[] bytes = (byte[]) entry.getValue();
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            toReturn.put(entry.getKey(), transfrom.apply(bytes));
        }
        return toReturn;
    }

    @Override
    public T put(T obj) {
        T afterPut = dbClient.put(obj);

        String memcacheKey = withNamespace(idGetter.getId(afterPut));
        byte[] bytes = this.memcacheTransform.to(afterPut);
        if (ArrayUtils.isNotEmpty(bytes)) {
            memcache.put(memcacheKey, bytes, DEFAULT_EXPIRATION);
        }
        memcache.delete(allCacheKey());

        return afterPut;
    }

    @Override
    public List<T> put(T... objs) {
        return put(Arrays.asList(objs));
    }

    @Override
    public List<T> put(List<T> objs) {
        List<T> afterPut = dbClient.put(objs);

        Map<String, byte[]> toPutMemcache = new HashMap<>();
        for (T t : afterPut) {
            String memcacheKey = withNamespace(idGetter.getId(t));
            byte[] bytes = this.memcacheTransform.to(t);
            if (ArrayUtils.isNotEmpty(bytes)) {
                toPutMemcache.put(memcacheKey, bytes);
            }
        }
        memcache.putAll(toPutMemcache, DEFAULT_EXPIRATION);
        memcache.delete(allCacheKey());

        return afterPut;
    }

    @Override
    public T getById(String id) {
        String memcacheKey = withNamespace(id);
        T fromMemcache = loadCache(memcacheKey, this.memcacheTransform::from);
        if (fromMemcache != null) {
            return fromMemcache;
        }
        T fromDb = dbClient.getById(id);
        if (fromDb != null) {
            byte[] bytes = this.memcacheTransform.to(fromDb);
            if (ArrayUtils.isNotEmpty(bytes)) {
                memcache.put(memcacheKey, bytes, DEFAULT_EXPIRATION);
            }
        }
        return fromDb;
    }

    @Override
    public Map<String, T> getByIds(List<String> ids) {
        List<String> memcacheKeys = ids.stream().map(this::withNamespace).collect(Collectors.toList());
        Map<String, T> fromMemcache = loadCache(memcacheKeys, this.memcacheTransform::from);
        List<String> toLoadFromDd = new ArrayList<>(CollectionUtils.subtract(ids, fromMemcache.keySet()));
        Map<String, T> fromDb = ImmutableMap.of();
        if (!toLoadFromDd.isEmpty()) {
            fromDb = dbClient.getByIds(toLoadFromDd);
            Map<String, byte[]> toPutIntoMemcache = fromDb.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            t -> withNamespace(t.getKey()),
                            t -> this.memcacheTransform.to(t.getValue())
                    ));
            memcache.putAll(toPutIntoMemcache, DEFAULT_EXPIRATION);
        }
        return ImmutableMap.<String, T>builder()
                .putAll(fromMemcache)
                .putAll(fromDb)
                .build();
    }

    @Override
    public void delete(String id) {
        dbClient.delete(id);
        memcache.delete(withNamespace(id));
        memcache.delete(allCacheKey());
    }

    @Override
    public void delete(String... ids) {
        delete(Arrays.asList(ids));
    }

    @Override
    public void delete(List<String> ids) {
        dbClient.delete(ids);
        List<String> memcacheKeys = ids.stream()
                .map(this::withNamespace)
                .collect(Collectors.toList());
        memcache.delete(memcacheKeys);
        memcache.delete(allCacheKey());
    }

    @Override
    public void deleteItem(T obj) {
        dbClient.deleteItem(obj);
        String memcacheKey = this.withNamespace(idGetter.getId(obj));
        memcache.delete(memcacheKey);
        memcache.delete(allCacheKey());
    }

    @Override
    public void deleteItems(List<T> objs) {
        dbClient.deleteItems(objs);
        List<String> memcacheKeys = objs.stream()
                .map(idGetter::getId)
                .map(this::withNamespace)
                .collect(Collectors.toList());
        memcache.delete(memcacheKeys);
        memcache.delete(allCacheKey());
    }

    @Override
    public Stream<T> scan() {
        if (!cacheCall) {
            return dbClient.scan();
        }

        // Load all cache keys
        List<T> fromMemcache = loadAllCache();
        if (CollectionUtils.isNotEmpty(fromMemcache)) {
            LOGGER.info("Scan results from memcache");
            return fromMemcache.stream();
        }

        LOGGER.info("Scan results from DB");
        List<T> fromDb = dbClient.scan().collect(Collectors.toList());

        saveAllCache(fromDb);

        return fromDb.stream();
    }

    private void saveAllCache(final List<T> items) {
        List<List<T>> partitions = Lists.partition(items, DEFAULT_PARTITION_SIZE);
        List<String> partitionKeys = new ArrayList<>();
        Map<String, byte[]> toPut = new HashMap<>();
        String allCacheKey = allCacheKey();
        for (List<T> partition : partitions) {
            String partitionKey = allCacheKey + "-" + UUID.randomUUID().toString();

            List<ByteString> byteStrings = partition.stream()
                    .map(this.memcacheTransform::to)
                    .filter(Objects::nonNull)
                    .map(ByteString::copyFrom)
                    .collect(Collectors.toList());
            BytesArray bytesArray = BytesArray.newBuilder()
                    .addAllValue(byteStrings)
                    .build();

            partitionKeys.add(partitionKey);
            toPut.put(partitionKey, bytesArray.toByteArray());
        }

        LOGGER.info("Refresh memcache. totalItems: {}. partitionCount: {}.", items.size(), partitionKeys.size());
        memcache.putAll(toPut, DEFAULT_EXPIRATION);
        memcache.put(allCacheKey, StringArray.newBuilder().addAllValue(partitionKeys).build().toByteArray(), DEFAULT_EXPIRATION);
    }

    private List<T> loadAllCache() {
        StringArray paritionKeys = loadCache(allCacheKey(), t -> {
            try {
                return StringArray.parseFrom(t);
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        });
        if (paritionKeys == null || paritionKeys.getValueCount() == 0) {
            return null;
        }

        List<T> toReturn = new ArrayList<>();

        for (String partitionKey : paritionKeys.getValueList()) {
            BytesArray bytesArray = loadCache(partitionKey, t -> {
                try {
                    return BytesArray.parseFrom(t);
                } catch (InvalidProtocolBufferException e) {
                    return null;
                }
            });
            if (bytesArray == null || bytesArray.getValueCount() == 0) {
                return null;
            }
            for (ByteString bytes : bytesArray.getValueList()) {
                T item = this.memcacheTransform.from(bytes.toByteArray());
                if (item != null) {
                    toReturn.add(item);
                }
            }
        }

        return toReturn;
    }

    //
    // TODO:
    // Currently, just fallback to delegated DBClient.
    // As we support cacheAll, we can leverage inMemoryQuery
    //
    @Override
    public Stream<T> queryInStream(DbQuery query) {
        LOGGER.error("You'd bettern not using it");
        return dbClient.queryInStream(query);
    }

    @Override
    public PaginatedResults<T> queryInPagination(int limit, PageToken pageToken) {
        LOGGER.error("You'd bettern not using it");
        return dbClient.queryInPagination(limit, pageToken);
    }

    @Override
    public PaginatedResults<T> queryInPagination(DbQuery query, int limit, PageToken pageToken) {
        LOGGER.error("You'd bettern not using it");
        return dbClient.queryInPagination(query, limit, pageToken);
    }
}
