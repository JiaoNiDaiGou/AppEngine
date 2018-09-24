package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.collect.ImmutableList;
import jiaonidaigou.appengine.common.model.BiTransform;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final Expiration DEFAULT_EXPIRATION = Expiration.byDeltaSeconds(60 * 60); //1h

    private final String namespace;
    private final MemcacheService memcache;
    private final DbClient<T> dbClient;
    private final IdGetter<T> idGetter;
    private final BiTransform<T, byte[]> memcacheTransform;

    public MemcacheDbClient(final String namespace,
                            final MemcacheService memcache,
                            final DbClient<T> dbClient,
                            final IdGetter<T> idGetter,
                            final BiTransform<T, byte[]> memcacheTransform) {
        this.namespace = checkNotNull(namespace);
        this.memcache = checkNotNull(memcache);
        this.dbClient = checkNotNull(dbClient);
        this.idGetter = checkNotNull(idGetter);
        this.memcacheTransform = checkNotNull(memcacheTransform);
    }

    private String withNamespace(String id) {
        return namespace + "." + id;
    }

    private void saveMemcache(final List<T> objs) {
        Map<String, byte[]> toPut = new HashMap<>();
        for (T obj : objs) {
            String id = withNamespace(idGetter.getId(obj));
            byte[] bytes = memcacheTransform.to(obj);
            if (bytes != null) {
                toPut.put(id, bytes);
            }
        }
        LOGGER.info("memcache put {}", toPut.keySet());
        memcache.putAll(toPut, DEFAULT_EXPIRATION);
    }

    private T loadMemcache(final String id) {
        String memcacheId = withNamespace(id);
        LOGGER.info("memcache load {}", memcacheId);
        byte[] bytes = (byte[]) memcache.get(memcacheId);
        if (bytes == null) {
            return null;
        }
        return memcacheTransform.from(bytes);
    }

    private void removeMemcache(final List<String> id) {
        List<String> memcacheIds = id.stream().map(this::withNamespace).collect(Collectors.toList());
        LOGGER.info("memcache remove {}", memcacheIds);
        memcache.deleteAll(memcacheIds);
    }

    @Override
    public T put(T obj) {
        T afterPut = dbClient.put(obj);
        saveMemcache(ImmutableList.of(afterPut));
        return afterPut;
    }

    @Override
    public List<T> put(T... objs) {
        return put(Arrays.asList(objs));
    }

    @Override
    public List<T> put(List<T> objs) {
        List<T> afterPut = dbClient.put(objs);
        saveMemcache(afterPut);
        return afterPut;
    }

    @Override
    public T getById(String id) {
        T fromMemcache = loadMemcache(id);
        if (fromMemcache != null) {
            return fromMemcache;
        }
        T fromDb = dbClient.getById(id);
        if (fromDb != null) {
            saveMemcache(ImmutableList.of(fromDb));
        }
        return fromDb;
    }

    @Override
    public void delete(String id) {
        dbClient.delete(id);
        memcache.delete(id);
    }

    @Override
    public void delete(String... ids) {
        delete(Arrays.asList(ids));
    }

    @Override
    public void delete(List<String> ids) {
        dbClient.delete(ids);
        removeMemcache(ids);
    }

    @Override
    public void deleteItem(T obj) {
        dbClient.deleteItem(obj);
        removeMemcache(ImmutableList.of(idGetter.getId(obj)));
    }

    @Override
    public void deleteItems(List<T> objs) {
        dbClient.deleteItems(objs);
        removeMemcache(objs.stream().map(idGetter::getId).collect(Collectors.toList()));
    }

    @Override
    public Stream<T> scan() {
        return dbClient.scan();
    }

    @Override
    public Stream<T> queryInStream(DbQuery query) {
        return dbClient.queryInStream(query);
    }

    @Override
    public PaginatedResults<T> queryInPagination(int limit, PageToken pageToken) {
        return dbClient.queryInPagination(limit, pageToken);
    }

    @Override
    public PaginatedResults<T> queryInPagination(DbQuery query, int limit, PageToken pageToken) {
        return dbClient.queryInPagination(query, limit, pageToken);
    }
}
