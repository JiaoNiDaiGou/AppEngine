package jiaoni.common.model;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class ProtoBytesBiTransform<T extends Message> implements BiTransform<T, byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtoBytesBiTransform.class);

    private final Parser<T> parser;

    public ProtoBytesBiTransform(final Parser<T> parser) {
        this.parser = checkNotNull(parser);
    }

    @Override
    public byte[] to(T t) {
        return t == null ? null : t.toByteArray();
    }

    @Override
    public T from(byte[] bytes) {
        try {
            return (bytes == null || bytes.length == 0) ? null : parser.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            LOGGER.error("failed to transform from bytes", e);
            return null;
        }
    }
}
