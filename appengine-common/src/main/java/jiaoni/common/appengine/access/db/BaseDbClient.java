package jiaoni.common.appengine.access.db;

import jiaoni.wiremodel.common.entity.PaginatedResults;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A delegated DbClient for actual client to extends.
 *
 * @param <T> Type of entity.
 */
public abstract class BaseDbClient<T> implements DbClient<T> {
    private final DbClient<T> client;

    public BaseDbClient(final DbClient client) {
        this.client = checkNotNull(client);
    }

    @Override
    public T put(T obj) {
        return client.put(obj);
    }

    @Override
    public List<T> put(T... objs) {
        return client.put(objs);
    }

    @Override
    public List<T> put(List<T> objs) {
        return client.put(objs);
    }

    @Override
    public T getById(String id) {
        return client.getById(id);
    }

    @Override
    public Map<String, T> getByIds(List<String> ids) {
        return client.getByIds(ids);
    }

    @Override
    public void delete(String id) {
        client.delete(id);
    }

    @Override
    public void delete(String... ids) {
        client.delete(ids);
    }

    @Override
    public void delete(List<String> ids) {
        client.delete(ids);
    }

    @Override
    public void deleteItem(T obj) {
        client.deleteItem(obj);
    }

    @Override
    public void deleteItems(List<T> objs) {
        client.deleteItems(objs);
    }

    @Override
    public Stream<T> scan() {
        return client.scan();
    }

    @Override
    public Stream<T> queryInStream(DbQuery query) {
        return client.queryInStream(query);
    }

    @Override
    public PaginatedResults<T> queryInPagination(int limit, PageToken pageToken) {
        return client.queryInPagination(limit, pageToken);
    }

    @Override
    public PaginatedResults<T> queryInPagination(DbQuery query, int limit, PageToken pageToken) {
        return client.queryInPagination(query, limit, pageToken);
    }
}
