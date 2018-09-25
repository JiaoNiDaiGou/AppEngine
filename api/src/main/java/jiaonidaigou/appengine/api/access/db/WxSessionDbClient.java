package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import jiaonidaigou.appengine.api.access.db.core.BaseDbClient;
import jiaonidaigou.appengine.api.access.db.core.BaseEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DbClientBuilder;
import jiaonidaigou.appengine.api.auth.WxSessionTicket;
import jiaonidaigou.appengine.api.utils.AppEnvironments;
import jiaonidaigou.appengine.common.model.Env;
import jiaonidaigou.appengine.common.utils.Environments;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WxSessionDbClient extends BaseDbClient<WxSessionTicket> {
    private static final String FIELD_OPEN_ID = "openId";
    private static final String FIELD_SESSION_KEY = "sessionId";
    private static final String FIELD_UNION_ID = "unionId";

    @Inject
    public WxSessionDbClient(final DatastoreService datastoreService,
                             final MemcacheService memcacheService) {
        super(new DbClientBuilder<WxSessionTicket>()
                .datastoreService(datastoreService)
                .memcacheService(memcacheService)
                .entityFactory(new EntityFactory(Environments.NAMESPACE_WX, AppEnvironments.ENV, "Session"))
                .useMemcacheJsonTransform("wx.sessionTicket", WxSessionTicket.class)
                .build());
    }

    private static final class EntityFactory extends BaseEntityFactory<WxSessionTicket> {
        protected EntityFactory(String serviceName, Env env, String tableName) {
            super(serviceName, env, tableName);
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
    }
}
