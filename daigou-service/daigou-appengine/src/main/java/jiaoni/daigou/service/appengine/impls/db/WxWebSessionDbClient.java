package jiaoni.daigou.service.appengine.impls.db;

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
import jiaoni.daigou.lib.wx.Session;
import jiaoni.daigou.service.appengine.AppEnvs;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WxWebSessionDbClient extends BaseDbClient<Session> {
    private static final String TABLE_NAME = "WxSession";
    private static final String FIELD_DATA = "data";

    @Inject
    public WxWebSessionDbClient(@ENV final Env env,
                                final DatastoreService datastoreService,
                                final MemcacheService memcacheService) {
        super(new DbClientBuilder<Session>()
                .datastoreService(datastoreService)
                .entityFactory(new EntityFactory(env))
                .memcacheService(memcacheService)
                .memcacheJsonTransform(TABLE_NAME, Session.class)
                .build());
    }

    private static class EntityFactory extends BaseEntityFactory<Session> {
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
        public Session fromEntity(DatastoreEntityExtractor entity) {
            return entity.getAsJson(FIELD_DATA, Session.class);
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, Session obj) {
            return partialBuilder.unindexedJson(FIELD_DATA, obj)
                    .build();
        }

        @Override
        public Session mergeId(Session obj, String id) {
            return obj;
        }

        @Override
        public String getId(Session obj) {
            return obj.getSessionId();
        }
    }
}
