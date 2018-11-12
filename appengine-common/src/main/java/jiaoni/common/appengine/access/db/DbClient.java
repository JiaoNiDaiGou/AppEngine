package jiaoni.common.appengine.access.db;

import jiaoni.wiremodel.common.entity.PaginatedResults;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface DbClient<T> {
    T put(final T obj);

    List<T> put(final T... objs);

    List<T> put(final List<T> objs);

    T getById(final String id);

    Map<String, T> getByIds(final List<String> ids);

    void delete(final String id);

    void delete(final String... ids);

    void delete(final List<String> ids);

    void deleteItem(final T obj);

    void deleteItems(final List<T> objs);

    Stream<T> scan();

    Stream<T> queryInStream(final DbQuery query);

    PaginatedResults<T> queryInPagination(@Nullable final DbQuery query, final int limit, @Nullable final PageToken pageToken);

    PaginatedResults<T> queryInPagination(@Nullable final DbQuery query, @Nullable final DbSort sort, final int limit, @Nullable final PageToken pageToken);

    /**
     * Extract the identifier of the object.
     */
    interface IdGetter<T> {
        String getId(final T obj);
    }
}
