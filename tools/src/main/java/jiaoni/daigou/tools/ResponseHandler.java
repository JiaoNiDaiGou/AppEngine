package jiaoni.daigou.tools;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import jiaoni.common.json.ObjectMapperProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

class ResponseHandler {
    static <T> T handle(final Response response, final Class<T> type) {
        System.out.println(response.getStatusInfo().getStatusCode() + ":" + response.getStatusInfo().getReasonPhrase());
        T toReturn = response.readEntity(type);
        System.out.println(toReturn);
        return toReturn;
    }

    static <T> T handle(final Response response, final GenericType<T> genericType) {
        System.out.println(response.getStatusInfo().getStatusCode() + ":" + response.getStatusInfo().getReasonPhrase());
        T toReturn = response.readEntity(genericType);
        System.out.println(toReturn);
        return toReturn;
    }

    static String handle(final Response response) {
        return handle(response, String.class);
    }

    /**
     * Assume request sent.
     */
    static <T> T handle(final HttpURLConnection connection, final Class<T> type) {
        try {
            System.out.println("HTTP response code: " + connection.getResponseCode() + " " + connection.getResponseMessage());
            if (connection.getErrorStream() != null) {
                try (InputStreamReader reader = new InputStreamReader(connection.getErrorStream(), Charsets.UTF_8)) {
                    String errorMessage = CharStreams.toString(reader);
                    System.out.println("error: " + errorMessage);
                }
            }
            try (InputStream inputStream = connection.getInputStream()) {
                T toReturn = ObjectMapperProvider.get().readValue(inputStream, type);
                System.out.println(toReturn);
                return toReturn;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String handle(final HttpURLConnection connection) {
        return handle(connection, String.class);
    }
}
