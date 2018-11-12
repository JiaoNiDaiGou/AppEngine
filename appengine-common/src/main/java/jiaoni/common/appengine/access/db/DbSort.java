package jiaoni.common.appengine.access.db;

import static com.google.common.base.Preconditions.checkNotNull;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

public final class DbSort {
    enum Direction {
        ASC, DESC
    }

    private final String field;
    private final Direction direction;

    public static DbSort asc(final String field) {
        return new DbSort(field, Direction.ASC);
    }

    public static DbSort desc(final String field) {
        return new DbSort(field, Direction.DESC);
    }

    private DbSort(final String field, final Direction direction) {
        this.field = checkNotBlank(field);
        this.direction = checkNotNull(direction);
    }

    String getField() {
        return field;
    }

    Direction getDirection() {
        return direction;
    }
}
