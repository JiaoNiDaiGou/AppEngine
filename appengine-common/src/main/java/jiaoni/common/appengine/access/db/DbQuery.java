package jiaoni.common.appengine.access.db;

import com.google.common.collect.Range;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Db query.
 */
public interface DbQuery {

    static DbQuery and(final List<DbQuery> queries) {
        if (queries.size() == 1) {
            return queries.get(0);
        }
        queries.forEach(t -> checkState(t.canComposited()));
        return new AndQuery(queries);
    }

    static DbQuery and(final DbQuery... queries) {
        return and(Arrays.asList(queries));
    }

    static DbQuery or(final List<DbQuery> queries) {
        if (queries.size() == 1) {
            return queries.get(0);
        }
        queries.forEach(t -> checkState(t.canComposited()));
        return new OrQuery(queries);
    }

    static DbQuery or(final DbQuery... queries) {
        return or(Arrays.asList(queries));
    }

    static KeyRangeQuery keyRange(final Range<String> range) {
        return new KeyRangeQuery(range);
    }

    static <T> SimpleQuery<T> eq(final String prop, final T val) {
        return new SimpleQuery<>(prop, QueryOp.EQ, val);
    }

    static <T> SimpleQuery<T> gt(final String prop, final T val) {
        return new SimpleQuery<>(prop, QueryOp.GT, val);
    }

    static <T> SimpleQuery<T> ge(final String prop, final T val) {
        return new SimpleQuery<>(prop, QueryOp.GE, val);
    }

    static <T> SimpleQuery<T> lt(final String prop, final T val) {
        return new SimpleQuery<>(prop, QueryOp.LT, val);
    }

    static <T> SimpleQuery<T> le(final String prop, final T val) {
        return new SimpleQuery<>(prop, QueryOp.LE, val);
    }

    static <T> SimpleQuery<T> notEq(final String prop, final T val) {
        return new SimpleQuery<>(prop, QueryOp.NEQ, val);
    }

    static <T> InMemoryQuery<T> inMemory(final Predicate<T> predicate) {
        return new InMemoryQuery<>(null, predicate);
    }

    static <T> InMemoryQuery<T> inMemory(final DbQuery dbQuery, final Predicate<T> predicate) {
        // We don't support multi-level in-memory DB
        checkState(!(dbQuery instanceof InMemoryQuery));
        return new InMemoryQuery<>(dbQuery, predicate);
    }

    boolean canComposited();

    enum QueryOp {
        EQ, GT, LT, GE, LE, NEQ
    }

    class InMemoryQuery<T> implements DbQuery {
        private final Predicate<T> predicate;
        private DbQuery dbQuery;

        InMemoryQuery(final DbQuery dbQuery, final Predicate<T> predicate) {
            if (dbQuery != null) {
                checkState(!(dbQuery instanceof InMemoryQuery), "The dbQuery cannot be inMemory");
            }
            this.predicate = checkNotNull(predicate);
            this.dbQuery = dbQuery;
        }

        Predicate<T> getPredicate() {
            return predicate;
        }

        DbQuery getDbQuery() {
            return dbQuery;
        }

        boolean hasDbQuery() {
            return dbQuery != null;
        }

        @Override
        public boolean canComposited() {
            return false;
        }
    }

    class AndQuery implements DbQuery {
        private final List<DbQuery> queries;

        AndQuery(final List<DbQuery> queries) {
            this.queries = queries;
        }

        List<DbQuery> getQueries() {
            return queries;
        }

        @Override
        public boolean canComposited() {
            return true;
        }
    }

    class OrQuery implements DbQuery {
        private final List<DbQuery> queries;

        OrQuery(final List<DbQuery> queries) {
            this.queries = queries;
        }

        List<DbQuery> getQueries() {
            return queries;
        }

        @Override
        public boolean canComposited() {
            return true;
        }
    }

    class SimpleQuery<T> implements DbQuery {
        private final String prop;
        private final QueryOp op;
        private final T val;

        SimpleQuery(final String prop, final QueryOp op, final T val) {
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

        @Override
        public boolean canComposited() {
            return true;
        }
    }

    class KeyRangeQuery implements DbQuery {
        private final Range<String> range;

        KeyRangeQuery(final Range<String> range) {
            this.range = checkNotNull(range);
        }

        public Range<String> getRange() {
            return range;
        }

        @Override
        public boolean canComposited() {
            return true;
        }
    }
}


