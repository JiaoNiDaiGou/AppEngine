package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.common.collect.Streams;
import jiaonidaigou.appengine.common.model.PaginatedResults;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOff;
import static jiaonidaigou.appengine.common.utils.LocalMeter.meterOn;

public class DatastoreClient<T> implements DbClient<T> {
    private final DatastoreEntityFactory<T> entityFactory;
    private final DatastoreService service;

    public DatastoreClient(final DatastoreService service,
                           final DatastoreEntityFactory<T> entityFactory) {
        this.entityFactory = entityFactory;
        this.service = service;
    }

    public static Query.Filter convertQueryFilter(final DbQuery query) {
        if (query instanceof AndDbQuery) {
            return new Query.CompositeFilter(Query.CompositeFilterOperator.AND,
                    ((AndDbQuery) query).getQueries().stream()
                            .map(DatastoreClient::convertQueryFilter)
                            .collect(Collectors.toList()));
        } else if (query instanceof OrDbQuery) {
            return new Query.CompositeFilter(Query.CompositeFilterOperator.OR,
                    ((OrDbQuery) query).getQueries().stream()
                            .map(DatastoreClient::convertQueryFilter)
                            .collect(Collectors.toList()));
        } else if (query instanceof SimpleDbQuery) {
            SimpleDbQuery simpleDbQuery = (SimpleDbQuery) query;
            switch (simpleDbQuery.getOp()) {
                case EQ:
                    return new Query.FilterPredicate(simpleDbQuery.getProp(),
                            Query.FilterOperator.EQUAL,
                            simpleDbQuery.getVal());
                case LE:
                    return new Query.FilterPredicate(simpleDbQuery.getProp(),
                            Query.FilterOperator.LESS_THAN_OR_EQUAL,
                            simpleDbQuery.getVal());
                case LT:
                    return new Query.FilterPredicate(simpleDbQuery.getProp(),
                            Query.FilterOperator.LESS_THAN,
                            simpleDbQuery.getVal());
                case GE:
                    return new Query.FilterPredicate(simpleDbQuery.getProp(),
                            Query.FilterOperator.GREATER_THAN_OR_EQUAL,
                            simpleDbQuery.getVal());
                case GT:
                    return new Query.FilterPredicate(simpleDbQuery.getProp(),
                            Query.FilterOperator.GREATER_THAN,
                            simpleDbQuery.getVal());
                default:
                    throw new UnsupportedOperationException("unexpected op " + simpleDbQuery);
            }
        } else {
            throw new UnsupportedOperationException("unexpected query " + query.getClass().getName());
        }
    }

    public DatastoreEntityFactory<T> getEntityFactory() {
        return entityFactory;
    }

    public String getKind() {
        return entityFactory.getKind();
    }

    @Override
    public T put(T obj) {
        checkNotNull(obj);
        meterOn(this.getClass());
        Entity entity = toEntity(obj);
        Key key = service.put(entity);
        T toReturn = entityFactory.mergeId(obj, extractId(key));
        meterOff();
        return toReturn;
    }

    @Override
    public List<T> put(List<T> objs) {
        checkNotNull(objs);
        meterOn(this.getClass());
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
        meterOn(this.getClass());
        T toReturn;
        try {
            Entity entity = service.get(buildKey(id));
            if (entity == null) {
                return null;
            }
            toReturn = entityFactory.fromEntity(DatastoreEntityExtractor.of(entity));
        } catch (EntityNotFoundException e) {
            toReturn = null;
        }
        meterOff();
        return toReturn;
    }

    @Override
    public void delete(final String id) {
        checkState(StringUtils.isNotBlank(id));
        meterOn(this.getClass());
        service.delete(buildKey(id));
        meterOff();
    }

    @Override
    public void delete(final T obj) {
        checkNotNull(obj);
        meterOn(this.getClass());
        delete(entityFactory.getId(obj));
        meterOff();
    }

    @Override
    public void delete(final List<String> ids) {
        ids.forEach(t -> checkState(StringUtils.isNotBlank(t)));
        meterOn(this.getClass());
        List<Key> keys = ids.stream()
                .map(this::buildKey)
                .collect(Collectors.toList());
        service.delete(keys);
        meterOff();
    }

    @Override
    public void deleteItems(final List<T> objs) {
        meterOn(this.getClass());
        List<String> ids = objs.stream()
                .map(entityFactory::getId)
                .collect(Collectors.toList());
        delete(ids);
        meterOff();
    }

    @Override
    public Stream<T> queryInStream(final DbQuery query) {
        meterOn(this.getClass());
        Query theQuery = new Query(entityFactory.getKind());
        if (query != null) {
            theQuery.setFilter(convertQueryFilter(query));
        }
        Stream<T> toReturn = Streams.stream(service.prepare(theQuery).asIterable())
                .map(DatastoreEntityExtractor::of)
                .map(entityFactory::fromEntity);
        meterOff();
        return toReturn;
    }

    @Override
    public PaginatedResults<T> queryInPagination(final DbQuery query,
                                                 final int limit,
                                                 final String nextToken) {
        Query theQuery = new Query(entityFactory.getKind());
        if (query != null) {
            theQuery.setFilter(convertQueryFilter(query));
        }

        FetchOptions fetchOptions = FetchOptions.Builder.withDefaults()
                .limit(limit);
        if (StringUtils.isNotBlank(nextToken)) {
            fetchOptions = fetchOptions.startCursor(Cursor.fromWebSafeString(nextToken));
        }
        QueryResultList<Entity> results = service.prepare(theQuery).asQueryResultList(fetchOptions);

        List<T> content = Streams.stream(results.iterator())
                .map(DatastoreEntityExtractor::of)
                .map(entityFactory::fromEntity)
                .collect(Collectors.toList());
        String newNextToken = results.getCursor() == null ? null : results.getCursor().toWebSafeString();
        if (nextToken != null && nextToken.equals(newNextToken)) {
            newNextToken = null;
        }
        return new PaginatedResults<>(content, newNextToken);
    }

    private Entity toEntity(final T obj) {
        if (obj == null) {
            return null;
        }
        Key key = buildKey(entityFactory.getId(obj));
        DatastoreEntityBuilder entityBuilder = new DatastoreEntityBuilder(key, getKind());
        return entityFactory.toEntity(entityBuilder, obj);
    }

    private Key buildKey(final String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        switch (entityFactory.getKeyType()) {
            case LONG_ID:
                return KeyFactory.createKey(getKind(), Long.parseLong(id));
            case STRING_NAME:
                return KeyFactory.createKey(getKind(), id);
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
}
