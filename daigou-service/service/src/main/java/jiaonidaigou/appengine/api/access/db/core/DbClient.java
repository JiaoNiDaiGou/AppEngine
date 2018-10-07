package jiaonidaigou.appengine.api.access.db.core;

import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;

import java.util.List;
import java.util.stream.Stream;

public interface DbClient<T> {
    T put(final T obj);

    List<T> put(final T... objs);

    List<T> put(final List<T> objs);

    T getById(final String id);

    void delete(final String id);

    void delete(final String... ids);

    void delete(final List<String> ids);

    void deleteItem(final T obj);

    void deleteItems(final List<T> objs);

    Stream<T> scan();

    Stream<T> queryInStream(final DbQuery query);

    PaginatedResults<T> queryInPagination(final int limit, final PageToken pageToken);

    PaginatedResults<T> queryInPagination(final DbQuery query, final int limit, final PageToken pageToken);

    /**
     * Extract the identifier of the object.
     */
    interface IdGetter<T> {
        String getId(final T obj);
    }
}
