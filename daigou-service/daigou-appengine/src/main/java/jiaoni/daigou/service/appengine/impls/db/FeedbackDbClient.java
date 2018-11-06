package jiaoni.daigou.service.appengine.impls.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DatastoreEntityFactory;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.access.db.DbQuery;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.daigou.service.appengine.AppEnvs;
import jiaoni.daigou.wiremodel.entity.sys.Feedback;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeedbackDbClient extends BaseDbClient<Feedback> {
    private static final String TABLE_NAME = "Feedback";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_OPEN = "open";

    @Inject
    public FeedbackDbClient(@ENV final Env env,
                            final DatastoreService service) {
        super(new DbClientBuilder<Feedback>()
                .datastoreService(service)
                .entityFactory(new EntityFactory(env))
                .build());
    }

    public List<Feedback> getAllOpenFeedbacks() {
        DbQuery dbQuery = DbQuery.eq(FIELD_OPEN, true);
        return this.queryInStream(dbQuery)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .collect(Collectors.toList());
    }

    private static class EntityFactory extends BaseEntityFactory<Feedback> {
        EntityFactory(Env env) {
            super(env);
        }

        @Override
        public KeyType getKeyType() {
            return DatastoreEntityFactory.KeyType.LONG_ID;
        }

        @Override
        public Feedback fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Feedback.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Feedback obj) {
            return partialBuilder
                    .indexedBoolean(FIELD_OPEN, obj.getOpen())
                    .unindexedProto(FIELD_DATA, obj)
                    .build();
        }

        @Override
        public Feedback mergeId(Feedback obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(Feedback obj) {
            return obj.getId();
        }

        @Override
        protected String getServiceName() {
            return AppEnvs.getServiceName();
        }

        @Override
        protected String getTableName() {
            return TABLE_NAME;
        }
    }
}
