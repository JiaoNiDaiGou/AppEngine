package jiaoni.common.appengine.access.db;

import jiaoni.common.model.Env;

import static com.google.common.base.Preconditions.checkNotNull;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

/**
 * A factory converts business logic object to Datastore entity, and vice versa.
 * It assume the business object has a String type of ID.
 *
 * @param <T> Type of business logic object.
 */
public abstract class BaseEntityFactory<T> implements DatastoreEntityFactory<T> {
    private final String kind;

    protected BaseEntityFactory(final Env env) {
        this.kind = checkNotBlank(getServiceName()) + "." + checkNotNull(env) + "." + checkNotBlank(getTableName());
    }

    protected BaseEntityFactory(final String serviceName, final Env env) {
        this.kind = checkNotBlank(serviceName) + "." + checkNotNull(env) + "." + checkNotBlank(getTableName());
    }

    protected abstract String getServiceName();

    protected abstract String getTableName();

    @Override
    public final String getKind() {
        return kind;
    }
}
