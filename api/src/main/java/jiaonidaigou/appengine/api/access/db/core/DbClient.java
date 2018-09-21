package jiaonidaigou.appengine.api.access.db.core;

import com.google.common.collect.Range;
import jiaonidaigou.appengine.wiremodel.entity.PaginatedResults;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

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

    enum QueryOp {
        EQ, GT, LT, GE, LE
    }

    interface DbQuery {
        static AndQuery and(final List<DbQuery> queries) {
            return new AndQuery(queries);
        }

        static OrQuery or(final List<DbQuery> queries) {
            return new OrQuery(queries);
        }

        static KeyRangeQuery rangeStringName(final Range<String> range) {
            return new KeyRangeQuery(range, null);
        }

        static KeyRangeQuery rangeLongId(final Range<Long> range) {
            return new KeyRangeQuery(null, range);
        }

        static <T> SimpleQuery eq(final String prop, final T val) {
            return new SimpleQuery<>(prop, QueryOp.EQ, val);
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
    }

    class AndQuery implements DbQuery {
        private final List<DbQuery> queries;

        AndQuery(final List<DbQuery> queries) {
            this.queries = queries;
        }

        List<DbQuery> getQueries() {
            return queries;
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
    }

    class KeyRangeQuery implements DbQuery {
        private final Range<String> stringNameRange;
        private final Range<Long> longIdRange;

        KeyRangeQuery(final Range<String> stringNameRange, final Range<Long> longIdRange) {
            checkArgument(stringNameRange == null || longIdRange == null);
            this.stringNameRange = stringNameRange;
            this.longIdRange = longIdRange;
        }

        public Range<String> getStringNameRange() {
            return stringNameRange;
        }

        public Range<Long> getLongIdRange() {
            return longIdRange;
        }
    }

    /**
     * Extract the identifier of the object.
     */
    interface IdGetter<T> {
        String getId(final T obj);
    }
}
