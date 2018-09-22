package jiaonidaigou.appengine.api.access.db.core;

import jiaonidaigou.appengine.api.access.db.core.DbQuery.InMemoryQuery;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class InMemoryDbClient<T> implements DbClient<T> {
    private final IdGetter<T> idGetter;
    private final BiFunction<T, String, T> idSetter;
    private final Map<String, T> map = new HashMap<>();

    public InMemoryDbClient(final IdGetter<T> idGetter,
                            final BiFunction<T, String, T> idSetter) {
        this.idGetter = checkNotNull(idGetter);
        this.idSetter = checkNotNull(idSetter);
    }

    private T enhanceId(T obj) {
        if (StringUtils.isNotBlank(idGetter.getId(obj))) {
            return obj;
        }
        String id = UUID.randomUUID().toString();
        return idSetter.apply(obj, id);
    }

    @Override
    public T put(T obj) {
        return put(Arrays.asList(obj)).get(0);
    }

    @Override
    public List<T> put(T... objs) {
        return put(Arrays.asList(objs));
    }

    @Override
    public List<T> put(List<T> objs) {
        return objs.stream()
                .map(this::enhanceId)
                .peek(t -> map.put(idGetter.getId(t), t))
                .collect(Collectors.toList());
    }

    @Override
    public T getById(String id) {
        return map.get(id);
    }

    @Override
    public void delete(String id) {
        map.remove(id);
    }

    @Override
    public void deleteItem(T obj) {
        map.remove(idGetter.getId(obj));
    }

    @Override
    public void delete(String... ids) {
        delete(Arrays.asList(ids));
    }

    @Override
    public void delete(List<String> ids) {
        ids.forEach(this::delete);
    }

    @Override
    public void deleteItems(List<T> objs) {
        objs.forEach(this::deleteItem);
    }

    @Override
    public Stream<T> scan() {
        return map.values().stream();
    }

    @Override
    public Stream<T> queryInStream(DbQuery query) {
        checkState(query instanceof InMemoryQuery);
        InMemoryQuery inMemoryQuery = (InMemoryQuery) query;
        return map.values().stream().filter(inMemoryQuery.getPredicate()::test);
    }

    @Override
    public PaginatedResults<T> queryInPagination(int limit, PageToken pageToken) {
        return queryInPagination(null, limit, pageToken);
    }

    @Override
    public PaginatedResults<T> queryInPagination(DbQuery query, int limit, PageToken pageToken) {
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
        return PaginatedResults.<T>builder()
                .withPageToken(newPageToken)
                .withTotoalCount(totalCount)
                .withResults(results)
                .build();
    }
}
