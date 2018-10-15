package jiaoni.songfan.service.appengine.impls;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.common.model.Env;
import jiaoni.songfan.service.appengine.AppEnvs;
import jiaoni.songfan.wiremodel.entity.Menu;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MenuDbClient extends BaseDbClient<Menu> {
    private static final String TABLE_NAME = "Menu";

    private static final String FIELD_DATA = "data";

    @Inject
    public MenuDbClient(@ENV final Env env,
                        final DatastoreService datastoreService,
                        final MemcacheService memcacheService) {
        super(new DbClientBuilder<Menu>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .memcacheProtoTransform(TABLE_NAME, Menu.parser())
                .memcacheService(memcacheService)
                .build());
    }

    private static class EntityFactory extends BaseEntityFactory<Menu> {
        EntityFactory(Env env) {
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
            return KeyType.STRING_NAME;
        }

        @Override
        public Menu fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsProtobuf(FIELD_DATA, Menu.parser());
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Menu obj) {
            return partialBuilder
                    .unindexedProto(FIELD_DATA, obj)
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public Menu mergeId(Menu obj, String id) {
            return obj.toBuilder().setId(id).build();
        }

        @Override
        public String getId(Menu obj) {
            return obj.getId();
        }
    }
}
