package jiaoni.songfan.service.appengine.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.songfan.service.appengine.AppEnvs;
import jiaoni.songfan.wiremodel.entity.Combo;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ComboDbClient extends BaseDbClient<Combo> {
    private static final String TABLE_NAME = "Combo";
    private static final String FIELD_DATA = "data";

    @Inject
    public ComboDbClient(@ENV final Env env,
                         final DatastoreService datastoreService) {
        super(new DbClientBuilder<Combo>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .build());
    }

    private static class EntityFactory extends BaseEntityFactory<Combo> {
        protected EntityFactory(Env env) {
            super(env);
        }

        @Override
        protected String getServiceName() {
            return AppEnvs.getServiceName();
        }

        @Override
        protected String getTableName() {
            return TABLE_NAME;
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.LONG_ID;
        }

        @Override
        public Combo fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Combo.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Combo obj) {
            return partialBuilder
                    .unindexedProto(FIELD_DATA, obj)
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public Combo mergeId(Combo obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(Combo obj) {
            return obj.getId();
        }
    }
}
