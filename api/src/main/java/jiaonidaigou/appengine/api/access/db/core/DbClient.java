package jiaonidaigou.appengine.api.access.db.core;

import jiaonidaigou.appengine.common.model.PaginatedResults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;

public interface DbClient<T> {
    T put(final T obj);

    default List<T> put(final T... objs) {
        checkState(objs != null);
        return put(Arrays.asList(objs));
    }

    List<T> put(final List<T> objs);

    T getById(final String id);

    void delete(final String id);

    void delete(final T obj);

    default void delete(final String... ids) {
        checkState(ids != null);
        delete(Arrays.asList(ids));
    }

    void delete(final List<String> ids);

    void deleteItems(final List<T> objs);

    default Stream<T> scan() {
        return queryInStream(null);
    }

    Stream<T> queryInStream(final DbQuery query);

    default PaginatedResults<T> queryInPagination(final int limit, final String nextToken) {
        return queryInPagination(null, limit, nextToken);
    }

    PaginatedResults<T> queryInPagination(final DbQuery query, final int limit, final String nextToken);

    enum QueryOp {
        EQ, GT, LT, GE, LE
    }

    interface DbQuery {
        static AndDbQuery and(final List<DbQuery> queries) {
            return new AndDbQuery(queries);
        }

        static OrDbQuery or(final List<DbQuery> queries) {
            return new OrDbQuery(queries);
        }

        static <T> SimpleDbQuery eq(final String prop, final T val) {
            return new SimpleDbQuery<>(prop, QueryOp.EQ, val);
        }
    }

    class AndDbQuery implements DbQuery {
        private final List<DbQuery> queries;

        AndDbQuery(final List<DbQuery> queries) {
            this.queries = queries;
        }

        List<DbQuery> getQueries() {
            return queries;
        }
    }

    class OrDbQuery implements DbQuery {
        private final List<DbQuery> queries;

        OrDbQuery(final List<DbQuery> queries) {
            this.queries = queries;
        }

        List<DbQuery> getQueries() {
            return queries;
        }
    }

    class SimpleDbQuery<T> implements DbQuery {
        private final String prop;
        private final QueryOp op;
        private final T val;

        SimpleDbQuery(final String prop, final QueryOp op, final T val) {
            this.prop = prop;
            this.op = op;
            this.val = val;
        }

        String getProp() {
            return prop;
        }

        QueryOp getOp() {
            return op;
        }

        T getVal() {
            return val;
        }
    }
}
