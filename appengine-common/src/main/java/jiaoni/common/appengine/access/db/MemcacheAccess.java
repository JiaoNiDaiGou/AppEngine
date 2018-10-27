package jiaoni.common.appengine.access.db;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import jiaoni.common.model.BiTransform;
import jiaoni.common.wiremodel.BytesArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Access to Memcache.
 * <p>
 * The key is always String.
 * The value is always bytes.
 */
class MemcacheAccess<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemcacheAccess.class);

    private static final int DEFAULT_PARTITION_SIZE = 200;

    private final MemcacheService memcache;
    private final String namespace;
    private final BiTransform<T, byte[]> transform;
    private final Expiration expiration;

    MemcacheAccess(final MemcacheService memcache,
                   final String namespace,
                   final BiTransform<T, byte[]> transform,
                   final Expiration expiration) {
        this.memcache = memcache;
        this.namespace = namespace;
        this.transform = transform;
        this.expiration = expiration;
    }

    /**
     * Delete certain key.
     */
    public void delete(final String key) {
        try {
            whenNotNull(withNs(key), memcache::delete);
        } catch (Exception e) {
            LOGGER.error("failed to delete. key={}", key);
        }
    }

    /**
     * Delete a list of keys.
     */
    public void delete(final String... keys) {
        delete(Arrays.asList(keys));
    }

    /**
     * Delete a list of keys.
     */
    public void delete(final Collection<String> keys) {
        try {
            if (CollectionUtils.isEmpty(keys)) {
                return;
            }
            memcache.deleteAll(keys.stream()
                    .map(this::withNs)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.error("failed to delete. keys={}", keys);
        }
    }

    /**
     * Put memcache by given key and value.
     */
    public void put(final String key, final T obj) {
        try {
            whenNoneNull(withNs(key), transformBytes(obj), (a, b) -> memcache.put(a, b, expiration));
        } catch (Exception e) {
            LOGGER.error("failed to put. key={}, obj={}", key, obj, e);
        }
    }

    /**
     * Put a map of [keys, values] into memcache.
     */
    public void putAll(final Map<String, T> map) {
        try {
            if (MapUtils.isEmpty(map)) {
                return;
            }
            Map<String, byte[]> toPut = new HashMap<>();
            for (Map.Entry<String, T> entry : map.entrySet()) {
                whenNoneNull(withNs(entry.getKey()), transformBytes(entry.getValue()), toPut::put);
            }
            if (!toPut.isEmpty()) {
                memcache.putAll(toPut, expiration);
            }
        } catch (Exception e) {
            LOGGER.error("failed to put map {}", map, e);
        }
    }

    /**
     * Put a collection of objects into single memecache blob.
     */
    public void putCollection(final String key, final Collection<T> collection) {
        try {
            if (StringUtils.isBlank(key) || CollectionUtils.isEmpty(collection)) {
                return;
            }
            BytesArray bytesArray = BytesArray.newBuilder()
                    .addAllValue(collection.stream()
                            .map(transform::to)
                            .map(ByteString::copyFrom)
                            .collect(Collectors.toList()))
                    .build();
            memcache.put(withNs(key), bytesArray.toByteArray(), expiration);
        } catch (Exception e) {
            LOGGER.error("failed to putCollection. key={}, collection={}", key, collection, e);
        }
    }

    /**
     * Put multiple collection into multiple shards.
     */
    public void putAllCollection(final Multimap<String, T> map) {
        try {
            if (map.isEmpty()) {
                return;
            }
            Map<String, byte[]> toPut = new HashMap<>();
            for (Map.Entry<String, Collection<T>> entry : map.asMap().entrySet()) {
                BytesArray bytesArray = BytesArray.newBuilder()
                        .addAllValue(entry.getValue()
                                .stream()
                                .map(transform::to)
                                .map(ByteString::copyFrom)
                                .collect(Collectors.toList()))
                        .build();
                toPut.put(withNs(entry.getKey()), bytesArray.toByteArray());
            }
            memcache.putAll(toPut, expiration);
        } catch (Exception e) {
            LOGGER.error("failed to putAllCollection. map.key={}", map.keySet());
        }
    }

    /**
     * Get values from a list of keys.
     */
    public Map<String, T> getAll(final Collection<String> keys) {
        try {
            if (CollectionUtils.isEmpty(keys)) {
                return ImmutableMap.of();
            }
            Map<String, Object> fromCache = memcache.getAll(
                    keys.stream()
                            .map(this::withNs)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
            Map<String, T> toReturn = new HashMap<>();
            for (String key : keys) {
                byte[] obj = (byte[]) fromCache.get(withNs(key));
                if (!empty(obj)) {
                    T t = transform.from(obj);
                    if (t != null) {
                        toReturn.put(key, t);
                    }
                }
            }
            return toReturn;
        } catch (Exception e) {
            LOGGER.error("failed to get all. keys={}", keys, e);
            return ImmutableMap.of();
        }
    }

    /**
     * Get data from certain blob as a collection of objects.
     */
    public List<T> getCollection(final String key) {
        try {
            byte[] bytes = (byte[]) memcache.get(withNs(key));
            if (empty(bytes)) {
                return ImmutableList.of();
            }
            BytesArray bytesArray = BytesArray.parseFrom(bytes);
            return bytesArray.getValueList()
                    .stream()
                    .map(t -> transform.from(t.toByteArray()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("failed to get collection by {}", key, e);
            return ImmutableList.of();
        }
    }

    /**
     * We load all items from DB, split them into several partitions, and then put them into different
     * Memcache blobs. And we also store these keys into a separate Memcache blob. So, once scan() is called,
     * we first check this blob to get all partitions keys. Then load all partitions.
     */
    public List<String> getShards() {
        try {
            byte[] bytes = (byte[]) memcache.get(sharedsKey());
            if (empty(bytes)) {
                return ImmutableList.of();
            }
            BytesArray bytesArray = BytesArray.parseFrom(bytes);
            return bytesArray.getValueList()
                    .stream()
                    .map(ByteString::toStringUtf8)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("failed to getShards. key={}", sharedsKey(), e);
            return ImmutableList.of();
        }
    }

    /**
     * Put shards
     */
    public void putShards(Collection<String> shards) {
        try {
            if (CollectionUtils.isEmpty(shards)) {
                return;
            }
            BytesArray bytesArray = BytesArray.newBuilder()
                    .addAllValue(shards.stream()
                            .map(ByteString::copyFromUtf8)
                            .collect(Collectors.toList()))
                    .build();
            memcache.put(sharedsKey(), bytesArray.toByteArray(), expiration);
        } catch (Exception e) {
            LOGGER.error("failed to setShards. key={}, shards={}", sharedsKey(), shards, e);
        }
    }

    /**
     * Delete shards.
     */
    public void deleteShards() {
        try {
            whenNotNull(sharedsKey(), memcache::delete);
        } catch (Exception e) {
            LOGGER.error("failed to delete. key={}", sharedsKey());
        }
    }


    /**
     * Put all items by leveraging shards.
     */
    public void putAllWithShards(final List<T> items) {
        putAllWithShards(items, DEFAULT_PARTITION_SIZE);
    }

    /**
     * Put all items by leveraging shards.
     */
    public void putAllWithShards(final List<T> items, final int partitionSize) {
        try {
            if (CollectionUtils.isEmpty(items) || partitionSize <= 0) {
                return;
            }

            List<List<T>> partitions = Lists.partition(items, partitionSize);
            List<String> shards = new ArrayList<>();
            Multimap<String, T> toPut = ArrayListMultimap.create();
            for (List<T> partition : partitions) {
                String shard = UUID.randomUUID().toString();
                shards.add(shard);
                toPut.putAll(shard, partition);
            }

            LOGGER.info("putAllWithShards. totalItems: {}. shardsCount: {}.", items.size(), shards.size());
            putAllCollection(toPut);
            putShards(shards);

        } catch (Exception e) {
            LOGGER.error("failed to putAllWithShards.", e);
        }
    }

    /**
     * Load all items by leveraging shards.
     */
    public List<T> getAllWithShards() {
        try {
            List<String> shards = getShards();
            if (CollectionUtils.isEmpty(shards)) {
                return ImmutableList.of();
            }
            List<T> toReturn = new ArrayList<>();
            for (String shard : shards) {
                List<T> partition = getCollection(shard);
                if (CollectionUtils.isNotEmpty(partition)) {
                    toReturn.addAll(partition);
                }
            }
            return toReturn;
        } catch (Exception e) {
            LOGGER.error("failed to getAllWithShards.", e);
            return ImmutableList.of();
        }
    }

    /**
     * Get value from given key.
     */
    public T get(final String key) {
        try {
            if (StringUtils.isBlank(key)) {
                return null;
            }
            byte[] bytes = (byte[]) memcache.get(withNs(key));
            return empty(bytes) ? null : transform.from(bytes);
        } catch (Exception e) {
            LOGGER.error("failed to get. key={}", key, e);
            return null;
        }
    }

    private String withNs(final String key) {
        return StringUtils.isBlank(key) ? null : namespace + "." + key;
    }

    private String sharedsKey() {
        return namespace + ".@ALL";
    }

    private byte[] transformBytes(final T t) {
        if (t == null) {
            return null;
        }
        byte[] bytes = transform.to(t);
        return empty(bytes) ? null : bytes;
    }

    private boolean empty(final byte[] bytes) {
        return bytes == null || bytes.length == 0;
    }

    private <A, B> void whenNoneNull(A a, B b, BiConsumer<A, B> consumer) {
        if (a != null || b != null) {
            consumer.accept(a, b);
        }
    }

    private <T> void whenNotNull(T t, Consumer<T> consumer) {
        if (t != null) {
            consumer.accept(t);
        }
    }
}
