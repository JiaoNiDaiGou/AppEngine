package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.base.Function;
import com.google.protobuf.ByteString;
import jiaonidaigou.appengine.common.model.BiTransform;
import jiaonidaigou.appengine.wiremodel.entity.BytesMap;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class MemcacheDbClient<T> implements DbClient<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemcacheDbClient.class);

    private final MemcacheService memcache;
    private final DbClient<T> dbClient;
    private final IdGetter<T> idGetter;
    private final Function<String, String> idToShard;
    private final BiTransform<T, byte[]> bytesTransform;

    private static class ShardTree {

    }

    /**
     * Where we save the memcache shard tree;
     */
    private static final String KEY_MEMCACHE_SHARDS_TREE = "_SHARD_TREE";

    /**
     * Max size we will save in single shard blob.
     * If one blob exceeds this limit, consider to separate it.
     */
    private static final int MAX_BLOB_SIZE = 960000;

    public MemcacheDbClient(final MemcacheService memcache,
                            final DbClient<T> dbClient,
                            final IdGetter<T> idGetter,
                            final Function<String, String> idToShard,
                            final BiTransform<T, byte[]> bytesTransform) {
        this.memcache = checkNotNull(memcache);
        this.dbClient = checkNotNull(dbClient);
        this.idToShard = checkNotNull(idToShard);
        this.bytesTransform = checkNotNull(bytesTransform);
        this.idGetter = checkNotNull(idGetter);
    }

    /**
     * Load memcache blob by given shard.
     */
    private Map<String, T> loadMemcacheBlob(final String shard) {
        byte[] bytes = (byte[]) memcache.get(shard);
        if (bytes == null) {
            return new HashMap<>();
        }
        try {
            return BytesMap.parseFrom(bytes)
                    .getValueMap()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            t -> bytesTransform.from(t.getValue().toByteArray())
                    ));
        } catch (Exception e) {
            LOGGER.error("failed to deserialize memcache data.", e);
            return new HashMap<>();
        }
    }

    /**
     * Save memcache blob with given shard.
     */
    private void saveMemcacheBlob(final String shard, final Map<String, T> blob) {
        byte[] bytes;
        try {
            bytes = BytesMap.newBuilder()
                    .putAllValue(blob.entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    t -> ByteString.copyFrom(bytesTransform.to(t.getValue()))
                            )))
                    .build()
                    .toByteArray();
        } catch (Exception e) {
            LOGGER.error("failed serialize memcache data.", e);
            return;
        }
        memcache.put(shard, bytes);
    }

    @Override
    public T put(T obj) {
        return null;
    }

    @Override
    public List<T> put(T... objs) {
        return null;
    }

    @Override
    public List<T> put(List<T> objs) {
        return null;
    }

    @Override
    public T getById(String id) {
        return null;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public void delete(T obj) {

    }

    @Override
    public void delete(String... ids) {

    }

    @Override
    public void delete(List<String> ids) {

    }

    @Override
    public void deleteItems(List<T> objs) {

    }

    @Override
    public Stream<T> scan() {
        return null;
    }

    @Override
    public Stream<T> queryInStream(DbQuery query) {
        return null;
    }

    @Override
    public PaginatedResults<T> queryInPagination(int limit, PageToken pageToken) {
        return null;
    }

    @Override
    public PaginatedResults<T> queryInPagination(DbQuery query, int limit, PageToken pageToken) {
        return null;
    }
}
