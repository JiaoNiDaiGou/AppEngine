package jiaoni.common.appengine.access.db;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Base64;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static jiaoni.common.utils.Preconditions2.checkNotBlank;

public class PageToken {
    private static final String DELIMITER = "|";
    private static final int PART_SIZE = 3;
    private final Source source;
    private final String token;
    private final int index;

    private PageToken(final Source source,
                      final String token,
                      final int index) {
        this.source = checkNotNull(source);
        switch (source) {
            case DATASTORE:
                checkNotBlank(token);
                break;
            case IN_MEMORY:
                checkArgument(index >= 0);
                break;
        }
        this.token = token;
        this.index = index;
    }

    public static PageToken fromPageToken(@Nullable final String pageToken) {
        if (StringUtils.isBlank(pageToken)) {
            return null;
        }

        String[] parts = StringUtils.split(new String(Base64.getDecoder().decode(pageToken), Charsets.UTF_8), DELIMITER);
        checkArgument(parts.length == PART_SIZE);

        Source source = Source.valueOf(parts[0]);
        String token = parts[1];
        int index = Integer.parseInt(parts[2]);

        return new PageToken(source, token, index);
    }

    public static PageToken datastore(final String token) {
        return new PageToken(Source.DATASTORE, token, 0);
    }

    public static PageToken inMemory(final int index) {
        return new PageToken(Source.IN_MEMORY, null, index);
    }

    public Source getSource() {
        return source;
    }

    public boolean isSourceInMemory() {
        return source == Source.IN_MEMORY;
    }

    public String getToken() {
        return token;
    }

    public int getIndex() {
        return index;
    }

    public String toPageToken() {
        return Base64.getEncoder().encodeToString(
                String.join(DELIMITER, source.name(), token, String.valueOf(index)).getBytes(Charsets.UTF_8));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public enum Source {
        IN_MEMORY,
        DATASTORE
    }
}
