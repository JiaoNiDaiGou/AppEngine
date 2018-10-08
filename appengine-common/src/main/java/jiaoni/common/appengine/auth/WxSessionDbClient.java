package jiaoni.common.appengine.auth;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import jiaoni.common.appengine.access.db.BaseDbClient;
import jiaoni.common.appengine.access.db.BaseEntityFactory;
import jiaoni.common.appengine.access.db.DatastoreEntityBuilder;
import jiaoni.common.appengine.access.db.DatastoreEntityExtractor;
import jiaoni.common.appengine.access.db.DbClientBuilder;
import jiaoni.common.model.Env;

public class WxSessionDbClient extends BaseDbClient<WxSessionTicket> {
    private static final String TABLE_NAME = "WxSession";
    private static final String FIELD_OPEN_ID = "openId";
    private static final String FIELD_SESSION_KEY = "sessionId";
    private static final String FIELD_UNION_ID = "unionId";

    public WxSessionDbClient(final String serviceName,
                             final Env env,
                             final DatastoreService datastoreService,
                             final MemcacheService memcacheService) {
        super(new DbClientBuilder<WxSessionTicket>()
                .datastoreService(datastoreService)
                .memcacheService(memcacheService)
                .entityFactory(new EntityFactory(serviceName, env))
                .memcacheJsonTransform("wx.sessionTicket", WxSessionTicket.class)
                .build());
    }

    private static final class EntityFactory extends BaseEntityFactory<WxSessionTicket> {
        protected EntityFactory(String serviceName, Env env) {
            super(env);
        }

        @Override
        public KeyType getKeyType() {
            return KeyType.STRING_NAME;
        }

        @Override
        public WxSessionTicket fromEntity(DatastoreEntityExtractor entity) {
            return WxSessionTicket.builder()
                    .withTicketId(entity.getKeyStringName())
                    .withOpenId(entity.getAsString(FIELD_OPEN_ID))
                    .withSessionKey(entity.getAsString(FIELD_SESSION_KEY))
                    .withUnionId(entity.getAsString(FIELD_UNION_ID))
                    .build();
        }

        @Override
        public Entity toEntity(DatastoreEntityBuilder partialBuilder, WxSessionTicket obj) {
            return partialBuilder
                    .unindexedString(FIELD_OPEN_ID, obj.getOpenId())
                    .unindexedString(FIELD_SESSION_KEY, obj.getSessionKey())
                    .unindexedString(FIELD_UNION_ID, obj.getUnionId())
                    .unindexedLastUpdatedTimestampAsNow()
                    .build();
        }

        @Override
        public WxSessionTicket mergeId(WxSessionTicket obj, String id) {
            return obj.toBuilder().withTicketId(id).build();
        }

        @Override
        public String getId(WxSessionTicket obj) {
            return obj.getTicketId();
        }

        @Override
        protected String getServiceName() {
            throw new IllegalStateException("shouldn't called");
        }

        @Override
        protected String getTableName() {
            return TABLE_NAME;
        }
    }
}
