package jiaonidaigou.appengine.api.access.db.core;

import com.google.common.collect.Range;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public interface DbClient<T> {
    T put(final T obj);

    List<T> put(final T... objs);

    List<T> put(final List<T> objs);

    T getById(final String id);

    void delete(final String id);

    void delete(final T obj);

    void delete(final String... ids);

    void delete(final List<String> ids);

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
