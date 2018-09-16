package jiaonidaigou.appengine.common.utils;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import jiaonidaigou.appengine.common.json.ObjectMapperProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper to make HTTPS calls with proper metrics and logging.
 */
public class MeteredHttpCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeteredHttpCall.class);

    private String serviceName;
    private String operation;
    private String requestId;
    private String url;
    private ConnectionPrepare connectionPrepare;

    public MeteredHttpCall() {
    }

    /**
     * Sets the service name you are calling. Used in metrics. Required.
     */
    public MeteredHttpCall setServiceName(final String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    /**
     * Sets the operation name you are calling. Used in metrics. Required.
     */
    public MeteredHttpCall setOperation(final String operation) {
        this.operation = operation;
        return this;
    }

    /**
     * Sets the request id for this call. Used in metrics. Optional.
     */
    public MeteredHttpCall setRequestId(final String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Sets the URL you are calling. Required.
     */
    public MeteredHttpCall setUrl(final String url) {
        this.url = url;
        return this;
    }

    /**
     * Prepare for the {@link HttpsURLConnection}.
     */
    public MeteredHttpCall prepareConnection(final ConnectionPrepare connectionPrepare) {
        this.connectionPrepare = connectionPrepare;
        return this;
    }

    public <T extends Message> T toProto(final Parser<T> parser) {
        try {
            return parser.parseFrom(toBytes());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T toJson(final Class<T> type) {
        try {
            return ObjectMapperProvider.get().readValue(toBytes(), type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Makes HTTPS call and get bytes.
     */
    public byte[] toBytes() {
        checkArgument(StringUtils.isNotBlank(serviceName));
        checkArgument(StringUtils.isNotBlank(operation));
        checkArgument(StringUtils.isNotBlank(url));
        checkNotNull(connectionPrepare);

        URL theUrl;
        try {
            theUrl = new URL(url);
        } catch (MalformedURLException e) {
//            metrics.increment(serviceName + ":" + operation + ":fail:malformedURL");
            String error = "Malformed URL " + url + debugMessage();
            LOGGER.error(error, e);
            throw new IllegalStateException(error, e);
        }

        HttpsURLConnection connection;
        try {
            connection = (HttpsURLConnection) theUrl.openConnection();
            connectionPrepare.prepare(connection);
        } catch (Exception e) {
//            metrics.increment(serviceName + ":" + operation + ":fail:setUpConnection");

            String error = "Failed to prepare the connection. " + debugMessage();
            LOGGER.error(error, e);
            throw new IllegalStateException(error, e);
        }

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {

//                metrics.increment(serviceName + ":" + operation + ":fail:" + responseCode);

                String error = "unknown error";
                if (connection.getErrorStream() != null) {
                    try (Reader reader = new InputStreamReader(connection.getErrorStream(), Charsets.UTF_8)) {
                        error = CharStreams.toString(reader);
                    }
                }
                error = "Call " + serviceName + " failed. " + debugMessage() + ", error=" + error;
                LOGGER.error(error);

                if (responseCode < 500) {
                    throw new IllegalStateException(error);
                } else {
                    throw new IllegalStateException(error);
                }
            } else {
//                metrics.increment(serviceName + ":" + operation + ":ok:" + responseCode);

                try (InputStream inputStream = connection.getInputStream()) {
                    return ByteStreams.toByteArray(inputStream);
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
//            metrics.increment(serviceName + ":" + operation + ":fail:unknown");
            String error = "Call " + serviceName + " failed. " + debugMessage();
            LOGGER.error(error);
            throw new IllegalStateException(e);
        }
    }

    private String debugMessage() {
        return String.format(" service=%s, operation=%s, url=%s, requestId=%s", serviceName, operation, url, requestId);
    }

    public interface ConnectionPrepare {
        void prepare(final HttpsURLConnection connection) throws Exception;
    }
}
