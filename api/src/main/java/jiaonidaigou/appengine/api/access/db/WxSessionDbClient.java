package jiaonidaigou.appengine.api.access.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import jiaonidaigou.appengine.api.access.db.core.BaseDbClient;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityBuilder;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityExtractor;
import jiaonidaigou.appengine.api.access.db.core.DatastoreEntityFactory;
import jiaonidaigou.appengine.api.access.db.core.DbClientBuilder;
import jiaonidaigou.appengine.api.auth.WxSessionTicket;
import jiaonidaigou.appengine.api.utils.AppEnvironments;
import jiaonidaigou.appengine.common.utils.Environments;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WxSessionDbClient extends BaseDbClient<WxSessionTicket> {
    private static final String KIND = Environments.SERVICE_NAME_WX + "." + AppEnvironments.ENV + ".Session";
    private static final String FIELD_OPEN_ID = "openId";
    private static final String FIELD_SESSION_KEY = "sessionId";
    private static final String FIELD_UNION_ID = "unionId";

    @Inject
    public WxSessionDbClient(final DatastoreService service) {
        super(new DbClientBuilder<WxSessionTicket>()
                .datastoreService(service)
                .entityFactory(new EntityFactory())
                .build());
    }

    private static final class EntityFactory implements DatastoreEntityFactory<WxSessionTicket> {
        @Override
        public KeyType getKeyType() {
            return KeyType.STRING_NAME;
        }

        @Override
        public String getKind() {
            return KIND;
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
