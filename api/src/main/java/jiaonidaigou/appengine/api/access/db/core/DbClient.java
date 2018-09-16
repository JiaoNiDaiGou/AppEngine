package jiaonidaigou.appengine.api.access.db.core;

import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    PaginatedResults<T> queryInPagination(final int limit, final PageToken nextToken);

    PaginatedResults<T> queryInPagination(final DbQuery query, final int limit, final PageToken nextToken);

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

        static <T> InMemoryQuery<T> inMemory(final Predicate<T> predicate) {
            return new InMemoryQuery<>(predicate);
        }
    }

    class InMemoryQuery<T> implements DbQuery {
        private final Predicate<T> predicate;

        InMemoryQuery(final Predicate<T> predicate) {
            this.predicate = predicate;
        }

        public Predicate<T> getPredicate() {
            return predicate;
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

    /**
     * Extract the identifier of the object.
     */
    interface IdGetter<T> {
        String getId(final T obj);
    }
}
