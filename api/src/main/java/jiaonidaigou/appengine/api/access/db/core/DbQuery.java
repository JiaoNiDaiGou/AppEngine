package jiaonidaigou.appengine.api.access.db.core;

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

    boolean canComposited();

    enum QueryOp {
        EQ, GT, LT, GE, LE
    }

    static AndQuery and(final List<DbQuery> queries) {
        queries.forEach(t -> checkState(t.canComposited()));
        return new AndQuery(queries);
    }

    static AndQuery and(final DbQuery... queries) {
        return and(Arrays.asList(queries));
    }

    static OrQuery or(final List<DbQuery> queries) {
        queries.forEach(t -> checkState(t.canComposited()));
        return new OrQuery(queries);
    }

    static OrQuery or(final DbQuery... queries) {
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

    static <T> InMemoryQuery<T> inMemory(final Predicate<T> predicate) {
        return new InMemoryQuery<>(predicate);
    }

    static <T> MemcacheQuery<T> memcache(final Predicate<T> predicate, final List<String> shards) {
        return new MemcacheQuery<>(shards, false, predicate);
    }

    static <T> MemcacheQuery<T> memcacheAllShards(final Predicate<T> predicate) {
        return new MemcacheQuery<>(null, true, predicate);
    }

    class MemcacheQuery<T> extends InMemoryQuery<T> {
        private final List<String> shards;
        private final boolean allShards;

        MemcacheQuery(final List<String> shards, final boolean allShards, Predicate<T> predicate) {
            super(predicate);
            this.shards = shards;
            this.allShards = allShards;
        }

        public List<String> getShards() {
            return shards;
        }

        public boolean isAllShards() {
            return allShards;
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


