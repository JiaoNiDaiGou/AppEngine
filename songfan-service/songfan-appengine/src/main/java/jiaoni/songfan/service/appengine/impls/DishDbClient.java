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
import jiaoni.songfan.wiremodel.entity.Dish;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DishDbClient extends BaseDbClient<Dish> {
    private static final String TABLE_NAME = "Dish";
    private static final String FIELD_DATA = "data";

    @Inject
    public DishDbClient(@ENV final Env env,
                        final DatastoreService datastoreService) {
        super(new DbClientBuilder<Dish>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .build());
    }

    private static class EntityFactory extends BaseEntityFactory<Dish> {
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
        public Dish fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Dish.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Dish obj) {
            return partialBuilder
                    .unindexedProto(FIELD_DATA, obj)
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public Dish mergeId(Dish obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(Dish obj) {
            return obj.getId();
        }
    }
}
