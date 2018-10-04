package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.common.annotations.VisibleForTesting;
import jiaonidaigou.appengine.api.access.db.core.BaseDbClient;
import jiaonidaigou.appengine.api.access.db.core.BaseEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DbClientBuilder;
import jiaonidaigou.appengine.api.access.db.core.DbQuery;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.wiremodel.entity.sys.Feedback;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.api.utils.AppEnvironments.ENV;

@Singleton
public class FeedbackDbClient extends BaseDbClient<Feedback> {
    private static final String TABLE_NAME = "Feedback";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_OPEN = "open";

    private static class EntityFactory extends BaseEntityFactory<Feedback> {

        protected EntityFactory(String serviceName, Env env, String tableName) {
            super(serviceName, env, tableName);
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public Feedback fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Feedback.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Feedback obj) {
            return partialBuilder
                    .unindexedBoolean(FIELD_OPEN, obj.getOpen())
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
    }

    @Inject
    public FeedbackDbClient(final DatastoreService service) {
        this(service, ENV);
    }

    @VisibleForTesting
    public FeedbackDbClient(final DatastoreService service, final Env env) {
        super(new DbClientBuilder<Feedback>()
                .datastoreService(service)
                .entityFactory(new EntityFactory(Environments.NAMESPACE_SYS, env, "Feedback"))
                .build());
    }

    public List<Feedback> getAllOpenFeedbacks() {
        DbQuery dbQuery = DbQuery.eq(FIELD_OPEN, true);
        return queryInStream(dbQuery)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .collect(Collectors.toList());
    }

    public void closeFeedback(final String id) {
        Feedback feedback = getById(id);
        if (feedback != null) {
            feedback = feedback.toBuilder().setOpen(false).build();
            put(feedback);
        }
    }
}
