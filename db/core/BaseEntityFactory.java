package jiaonidaigou.appengine.api.access.db.core;

import jiaonidaigou.appengine.common.model.Env;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A factory converts business logic object to Datastore entity, and vice versa.
 * It assume the business object has a String type of ID.
 *
 * @param <T> Type of business logic object.
 */
public abstract class BaseEntityFactory<T> implements DatastoreEntityFactory<T> {
    private final String kind;

    protected BaseEntityFactory(final String serviceName, final Env env, final String tableName) {
        checkState(StringUtils.isNotBlank(serviceName));
        checkNotNull(env);
        checkState(StringUtils.isNotBlank(tableName));
        this.kind = serviceName + "." + env + "." + tableName;
    }

    @Override
    public final String getKind() {
        return kind;
    }
}
