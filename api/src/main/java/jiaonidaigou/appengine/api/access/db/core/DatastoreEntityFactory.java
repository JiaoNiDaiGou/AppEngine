package jiaonidaigou.appengine.api.access.db.core;

import com.google.appengine.api.datastore.Entity;

/**
 * A factory converts business logic object to Datastore entity, and vice versa.
 * It assume the business object has a String type of ID.
 *
 * @param <T> Type of business logic object.
 */
public interface DatastoreEntityFactory<T> extends DbClient.IdGetter<T> {
    KeyType getKeyType();

    /**
     * @return The kind of the Datastore.
     */
    String getKind();

    /**
     * Transform Datastore entity into business logic object.
     */
    T fromEntity(final DatastoreEntityExtractor entity);

    /**
     * Transform business logic object into Datastore entity.
     */
    Entity toEntity(final DatastoreEntityBuilder partialBuilder, final T obj);

    /**
     * Merges the business logic item with a new Id.
     */
    T mergeId(final T obj, final String id);

    /**
     * Type of Datastore entity key.
     */
    enum KeyType {
        STRING_NAME, LONG_ID
    }
}
