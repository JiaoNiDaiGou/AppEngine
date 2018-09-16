package jiaonidaigou.appengine.api.access.db.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOff;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOn;

/**
 * An implementation of {@link DbClient} using in-memory cache.
 *
 * @param <T> Entity type.
 */
public class InMemoryCacheDbClient<T> implements DbClient<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryCacheDbClient.class);

    private static final int DEFAULT_MAX_SIZE = 5000;

    private final LoadingCache<String, T> cache;
    private final DbClient<T> dbClient;
    private final IdGetter<T> idGetter;

    public InMemoryCacheDbClient(final DbClient<T> dbClient,
                                 final IdGetter<T> idGetter) {
        this.dbClient = checkNotNull(dbClient);
        this.idGetter = checkNotNull(idGetter);
        this.cache = CacheBuilder.<String, T>newBuilder()
                .maximumSize(DEFAULT_MAX_SIZE)
                .expireAfterWrite(10, TimeUnit.DAYS)
                .build(new CacheLoader<String, T>() {
                    @Override
                    public T load(String key) {
                        return dbClient.getById(key);
                    }
                });
    }

    public InMemoryCacheDbClient(final DbClient<T> dbClient,
                                 final IdGetter<T> idGetter,
                                 final CacheBuilder<String, T> cache) {
        this.dbClient = checkNotNull(dbClient);
        this.idGetter = checkNotNull(idGetter);
        this.cache = checkNotNull(cache)
                .build(new CacheLoader<String, T>() {
                    @Override
                    public T load(String key) {
                        return dbClient.getById(key);
                    }
                });
    }

    @Override
    public T put(T obj) {
        T afterPut = dbClient.put(obj);
        cache.put(idGetter.getId(afterPut), afterPut);
        return afterPut;
    }

    @Override
    public List<T> put(T... objs) {
        return put(Arrays.asList(objs));
    }

    @Override
    public List<T> put(List<T> objs) {
        List<T> aferPut = dbClient.put(objs);
        Map<String, T> toPutInCache = new HashMap<>();
        for (T t : aferPut) {
            toPutInCache.put(idGetter.getId(t), t);
        }
        cache.putAll(toPutInCache);
        return aferPut;
    }

    @Override
    public T getById(String id) {
        T toReturn = cache.getIfPresent(id);
        if (toReturn == null) {
            toReturn = dbClient.getById(id);
            if (toReturn != null) {
                cache.put(id, toReturn);
            }
        }
        return toReturn;
    }

    @Override
    public void delete(String id) {
        dbClient.delete(id);
        cache.invalidate(id);
    }

    @Override
    public void delete(T obj) {
        dbClient.delete(obj);
        cache.invalidate(idGetter.getId(obj));
    }

    @Override
    public void delete(String... ids) {
        delete(Arrays.asList(ids));
    }

    @Override
    public void delete(List<String> ids) {
        dbClient.delete(ids);
        cache.invalidateAll(ids);
    }

    @Override
    public void deleteItems(List<T> objs) {
        dbClient.deleteItems(objs);
        List<String> ids = objs.stream().map(idGetter::getId).collect(Collectors.toList());
        cache.invalidateAll(ids);
    }

    @Override
    public Stream<T> scan() {
        if (cache.size() == 0) {
            refreshCacheAll();
        }
        return cache.asMap().entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue);
    }

    @Override
    public Stream<T> queryInStream(DbQuery query) {
        Stream<T> toReturn;
        if (query != null) {
            checkState(query instanceof InMemoryQuery);
            toReturn = scan().filter(((InMemoryQuery<T>) query).getPredicate());
        } else {
            toReturn = scan();
        }
        return toReturn;
    }

    @Override
    public PaginatedResults<T> queryInPagination(int limit, PageToken nextToken) {
        return queryInPagination(null, limit, nextToken);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaginatedResults<T> queryInPagination(final DbQuery query, final int limit, final PageToken pageToken) {
        meterOn();
        LOGGER.info("Query in page: limit={}, pageToken={}", limit, pageToken);

        int fromIndex = 0;
        if (pageToken != null) {
            checkState(pageToken.getSource() == PageToken.Source.IN_MEMORY);
            fromIndex = pageToken.getIndex();
        }

        List<T> results = queryInStream(query).collect(Collectors.toList());
        final int totalCount = results.size();

        int toIndex = Math.min(fromIndex + limit, results.size());

        if (fromIndex != 0 || toIndex != results.size()) {
            results = results.subList(fromIndex, toIndex);
        }

        String newPageToken = toIndex >= results.size() ? null : PageToken.inMemory(toIndex).toPageToken();
        PaginatedResults<T> toReturn = PaginatedResults.<T>builder()
                .withPageToken(newPageToken)
                .withTotoalCount(totalCount)
                .withResults(results)
                .build();

        meterOff();
        return toReturn;
    }

    public void refreshCacheAll() {
        List<T> all = dbClient.scan().collect(Collectors.toList());
        Map<String, T> toPutInCache = new HashMap<>();
        for (T t : all) {
            toPutInCache.put(idGetter.getId(t), t);
        }
        cache.putAll(toPutInCache);
    }
}
