package jiaoni.common.test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import jiaoni.common.model.InternalIOException;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Stubber;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestUtils {
    public static String readResourcesAsString(final String resourceName) {
        try (Reader reader = new InputStreamReader(Resources.getResource(resourceName).openStream(), Charsets.UTF_8)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public static File readResourceAsFile(final String resourceName) {
        return new File(Resources.getResource(resourceName).getFile());
    }

    public static byte[] readResourceAsBytes(final String resourceName) {
        try {
            return ByteStreams.toByteArray(Resources.getResource(resourceName).openStream());
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public static ExpectedHttpRequest readResourceAsExpectedHttpRequest(final String resourceName) {
        try {
            return ExpectedHttpRequest.fromReqFile(Resources.getResource(resourceName));
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public static Stubber doReturnStringFromResource(final String... resourceNames) {
        if (resourceNames.length == 1) {
            return doReturn(readResourcesAsString(resourceNames[0]));
        } else {
            String[] otherResponses = Arrays.stream(resourceNames, 1, resourceNames.length)
                    .map(TestUtils::readResourcesAsString)
                    .toArray(String[]::new);
            return doReturn(readResourcesAsString(resourceNames[0]), (Object[]) otherResponses);
        }
    }

    public static void verifyHttpExecute(final HttpClient client,
                                         final ExpectedHttpRequest... requests) {
        ArgumentCaptor<HttpUriRequest> argumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        try {
            verify(client, times(requests.length)).execute(argumentCaptor.capture(), any(ResponseHandler.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<HttpUriRequest> requestsExecuted = argumentCaptor.getAllValues();
        for (int i = 0; i < requests.length; i++) {
            ExpectedHttpRequest expected = requests[i];
            HttpUriRequest actual = requestsExecuted.get(i);

            // Verify method
            assertEquals(expected.getMethod(), actual.getMethod());

            // Verify URL
            assertEquals(expected.getUrl(), actual.getURI().toString());

            // Verify headers
            assertEquals(expected.getHeaders().size(), actual.getAllHeaders().length);
            for (Map.Entry<String, Collection<String>> entry : expected.getHeaders().asMap().entrySet()) {
                String headerName = entry.getKey();
                List<String> expectedHeaderValues = entry.getValue()
                        .stream()
                        .map(String::trim)
                        .sorted()
                        .collect(Collectors.toList());
                List<String> actualHeaderValues = Arrays.stream(actual.getHeaders(headerName))
                        .map(NameValuePair::getValue)
                        .map(String::trim)
                        .sorted()
                        .collect(Collectors.toList());
                assertEquals(expectedHeaderValues, actualHeaderValues);
            }

//            // Verify Cookie
//            if (expected.getCookie() != null) {
//                assertEquals(expected.getCookie(), actual.getRequestLine().);
//            }
        }
    }
}
