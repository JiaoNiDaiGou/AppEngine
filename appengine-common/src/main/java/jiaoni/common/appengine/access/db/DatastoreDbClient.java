package jiaoni.common.appengine.access.db;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import jiaoni.common.appengine.access.db.DbQuery.AndQuery;
import jiaoni.common.appengine.access.db.DbQuery.InMemoryQuery;
import jiaoni.common.appengine.access.db.DbQuery.KeyRangeQuery;
import jiaoni.common.appengine.access.db.DbQuery.OrQuery;
import jiaoni.common.appengine.access.db.DbQuery.SimpleQuery;
import jiaoni.wiremodel.common.entity.PaginatedResults;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import static com.google.appengine.api.datastore.Query.CompositeFilterOperator.AND;
import static com.google.appengine.api.datastore.Query.CompositeFilterOperator.OR;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN;
import static com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.NOT_EQUAL;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static jiaoni.common.utils.LocalMeter.meterOff;
import static jiaoni.common.utils.LocalMeter.meterOn;

/**
 * Implementation of {@link DbClient} by calling Datastore.
 *
 * @param <T> Type of entity
 */
public class DatastoreDbClient<T> implements DbClient<T> {
    private static final String KEY_PROP = "__key__";

    private final DatastoreEntityFactory<T> entityFactory;
    private final DatastoreService service;

    public DatastoreDbClient(final DatastoreService service,
                             final DatastoreEntityFactory<T> entityFactory) {
        this.entityFactory = checkNotNull(entityFactory);
        this.service = checkNotNull(service);
    }

    @Override
    public T put(T obj) {
        checkNotNull(obj);
        meterOn();

        Entity entity = toEntity(obj);
        Key key = service.put(entity);
        T toReturn = entityFactory.mergeId(obj, extractId(key));

        meterOff();
        return toReturn;
    }

    @Override
    public List<T> put(T... objs) {
        checkState(objs != null);
        return put(Arrays.asList(objs));
    }

    @Override
    public List<T> put(List<T> objs) {
        checkNotNull(objs);
        meterOn();

        List<Entity> entities = objs.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        List<Key> keys = service.put(entities);
        List<T> toReturn = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            toReturn.add(entityFactory.mergeId(objs.get(i), extractId(keys.get(i))));
        }

        meterOff();
        return toReturn;
    }

    @Override
    public T getById(String id) {
        checkState(StringUtils.isNotBlank(id));
        meterOn();

        T toReturn;
        try {
            Entity entity = service.get(buildKey(id));
            if (entity == null) {
                return null;
            }
            toReturn = toObj(entity);
        } catch (EntityNotFoundException e) {
            toReturn = null;
        }

        meterOff();
        return toReturn;
    }

    @Override
    public Map<String, T> getByIds(List<String> ids) {
        checkState(CollectionUtils.isNotEmpty(ids));
        ids.forEach(t -> checkState(StringUtils.isNotBlank(t)));

        meterOn();
        List<Key> keys = ids.stream().map(this::buildKey).collect(Collectors.toList());
        Map<String, T> toReturn = service.get(keys)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(t -> extractId(t.getKey()), t -> toObj(t.getValue())));
        meterOff();
        return toReturn;
    }

    @Override
    public void delete(final String id) {
        checkState(StringUtils.isNotBlank(id));
        meterOn();

        service.delete(buildKey(id));

        meterOff();
    }

    @Override
    public void deleteItem(final T obj) {
        checkNotNull(obj);
        meterOn();

        delete(entityFactory.getId(obj));

        meterOff();
    }

    @Override
    public void delete(String... ids) {
        checkState(ids != null);
        delete(Arrays.asList(ids));
    }

    @Override
    public void delete(final List<String> ids) {
        ids.forEach(t -> checkState(StringUtils.isNotBlank(t)));
        meterOn();

        List<Key> keys = ids.stream()
                .map(this::buildKey)
                .collect(Collectors.toList());
        service.delete(keys);

        meterOff();
    }

    @Override
    public void deleteItems(final List<T> objs) {
        meterOn();

        List<String> ids = objs.stream()
                .map(entityFactory::getId)
                .collect(Collectors.toList());
        delete(ids);

        meterOff();
    }

    @Override
    public Stream<T> scan() {
        return queryInStream(null);
    }

    @Override
    public Stream<T> queryInStream(final DbQuery query) {
        meterOn();

        Query theQuery = new Query(entityFactory.getKind());
        Predicate<T> inMemoryPredicate = t -> true;
        if (query != null) {
            theQuery.setFilter(convertQueryFilter(query));
            inMemoryPredicate = convertInMemoryPredicate(query);
        }
        Stream<T> toReturn = Streams.stream(service.prepare(theQuery)
                .asIterable())
                .map(this::toObj)
                .filter(inMemoryPredicate);

        meterOff();
        return toReturn;
    }

    @Override
    public PaginatedResults<T> queryInPagination(final DbQuery query,
                                                 final int limit,
                                                 final PageToken pageToken) {
        return queryInPagination(query, null, limit, pageToken);
    }

    @Override
    public PaginatedResults<T> queryInPagination(DbQuery query, DbSort sort, int limit, @Nullable PageToken pageToken) {
        meterOn();

        Query theQuery = new Query(entityFactory.getKind());
        Predicate<T> inMemoryPredicate = t -> true;
        if (query != null) {
            theQuery.setFilter(convertQueryFilter(query));
            inMemoryPredicate = convertInMemoryPredicate(query);
        }
        if (sort != null) {
            theQuery.addSort(sort.getField(), convertSortDirection(sort.getDirection()));
        }

        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(limit);
        if (pageToken != null) {
            checkState(pageToken.getSource() == PageToken.Source.DATASTORE);
            fetchOptions = fetchOptions.startCursor(Cursor.fromWebSafeString(pageToken.getToken()));
        }
        QueryResultList<Entity> results = service.prepare(theQuery).asQueryResultList(fetchOptions);

        List<T> content = results.stream()
                .map(this::toObj)
                .filter(inMemoryPredicate)
                .collect(Collectors.toList());

        String newNextToken = results.getCursor() == null ? null : results.getCursor().toWebSafeString();
        if (StringUtils.isBlank(newNextToken)) {
            newNextToken = null;
        }
        if (pageToken != null && pageToken.getToken().equals(newNextToken)) {
            newNextToken = null;
        }

        String newPageTokenStr = newNextToken == null ? null : PageToken.datastore(newNextToken).toPageToken();

        meterOff();

        return PaginatedResults.<T>builder()
                .withResults(content)
                .withPageToken(newPageTokenStr)
                .build();
    }

    private T toObj(final Entity entity) {
        Key key = entity.getKey();
        DatastoreEntityExtractor extractor = DatastoreEntityExtractor.of(entity);
        T obj = entityFactory.fromEntity(extractor);
        obj = entityFactory.mergeId(obj, extractId(key));
        return obj;
    }

    private Entity toEntity(final T obj) {
        if (obj == null) {
            return null;
        }
        Key key = buildKey(entityFactory.getId(obj));
        DatastoreEntityBuilder entityBuilder = new DatastoreEntityBuilder(key, entityFactory.getKind());
        return entityFactory.toEntity(entityBuilder, obj);
    }

    /**
     * Build up Datastore key, from given string format of ID.
     */
    private Key buildKey(@Nullable final String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        switch (entityFactory.getKeyType()) {
            case LONG_ID:
                return KeyFactory.createKey(entityFactory.getKind(), Long.parseLong(id));
            case STRING_NAME:
                return KeyFactory.createKey(entityFactory.getKind(), id);
            default:
                throw new IllegalStateException("Unexpected key type " + entityFactory.getKeyType());
        }
    }

    private String extractId(final Key key) {
        if (key == null) {
            return null;
        }
        switch (entityFactory.getKeyType()) {
            case LONG_ID:
                return String.valueOf(key.getId());
            case STRING_NAME:
                return key.getName();
            default:
                throw new IllegalStateException("Unexpected key type " + entityFactory.getKeyType());
        }
    }

    /**
     * Converts {@link AndQuery}.
     */
    private Filter convertAndQuery(final AndQuery query) {
        return new Query.CompositeFilter(AND, query.getQueries()
                .stream()
                .map(this::convertQueryFilter)
                .collect(Collectors.toList()));
    }

    /**
     * Converts {@link OrQuery}.
     */
    private Filter convertOrQuery(final OrQuery query) {
        return new Query.CompositeFilter(OR, query.getQueries()
                .stream()
                .map(this::convertQueryFilter)
                .collect(Collectors.toList()));
    }

    private Query.SortDirection convertSortDirection(final DbSort.Direction direction) {
        switch (direction) {
            case ASC:
                return Query.SortDirection.ASCENDING;
            case DESC:
                return Query.SortDirection.DESCENDING;
            default:
                throw new UnsupportedOperationException("unexpected direction " + direction);
        }
    }

    /**
     * Converts {@link SimpleQuery}.
     */
    private Filter convertSimpleQuery(final SimpleQuery query) {
        switch (query.getOp()) {
            case EQ:
                return new Query.FilterPredicate(query.getProp(), EQUAL, query.getVal());
            case LE:
                return new Query.FilterPredicate(query.getProp(), LESS_THAN_OR_EQUAL, query.getVal());
            case LT:
                return new Query.FilterPredicate(query.getProp(), LESS_THAN, query.getVal());
            case GE:
                return new Query.FilterPredicate(query.getProp(), GREATER_THAN_OR_EQUAL, query.getVal());
            case GT:
                return new Query.FilterPredicate(query.getProp(), GREATER_THAN, query.getVal());
            case NEQ:
                return new Query.FilterPredicate(query.getProp(), NOT_EQUAL, query.getVal());
            default:
                throw new UnsupportedOperationException("unexpected op " + query);
        }
    }

    /**
     * Converts {@link KeyRangeQuery}.
     */
    private Filter convertKeyRangeQuery(final KeyRangeQuery query) {
        Query.FilterPredicate lowerFilter = null;
        Query.FilterPredicate upperFilter = null;
        Range<String> range = query.getRange();
        if (range.hasLowerBound()) {
            Query.FilterOperator op = range.lowerBoundType() == BoundType.CLOSED
                    ? GREATER_THAN_OR_EQUAL
                    : GREATER_THAN;
            lowerFilter = new Query.FilterPredicate(KEY_PROP, op, buildKey(range.lowerEndpoint()));
        }
        if (range.hasUpperBound()) {
            Query.FilterOperator op = range.upperBoundType() == BoundType.CLOSED
                    ? LESS_THAN_OR_EQUAL
                    : LESS_THAN;
            upperFilter = new Query.FilterPredicate(KEY_PROP, op, buildKey(range.upperEndpoint()));
        }
        if (lowerFilter == null && upperFilter == null) {
            return null;
        } else if (lowerFilter == null) {
            return upperFilter;
        } else if (upperFilter == null) {
            return lowerFilter;
        }
        Filter toReturn = new Query.CompositeFilter(AND, Arrays.asList(lowerFilter, upperFilter));
        System.out.println(toReturn);
        return toReturn;
    }

    /**
     * Converts {@link DbQuery}.
     */
    private Filter convertQueryFilter(final DbQuery query) {
        if (query instanceof InMemoryQuery) {
            InMemoryQuery inMemoryQuery = (InMemoryQuery) query;
            return inMemoryQuery.getDbQuery() == null ? null : convertQueryFilter(inMemoryQuery.getDbQuery());
        } else if (query instanceof AndQuery) {
            return convertAndQuery((AndQuery) query);
        } else if (query instanceof OrQuery) {
            return convertOrQuery((OrQuery) query);
        } else if (query instanceof KeyRangeQuery) {
            return convertKeyRangeQuery((KeyRangeQuery) query);
        } else if (query instanceof SimpleQuery) {
            return convertSimpleQuery((SimpleQuery) query);
        } else {
            throw new IllegalStateException("unexpected query " + query.getClass().getName());
        }
    }

    private Predicate<T> convertInMemoryPredicate(final DbQuery query) {
        if (query instanceof InMemoryQuery) {
            return ((InMemoryQuery) query).getPredicate();
        }
        return t -> true;
    }
}
