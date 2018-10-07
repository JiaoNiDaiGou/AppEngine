package jiaoni.common.test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.common.model.InternalIOException;
import org.mockito.stubbing.Stubber;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

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

    public static MockBrowserClient mockBrowserClient() {
        MockBrowserClient toReturn = mock(MockBrowserClient.class);
        doCallRealMethod().when(toReturn).doPost();
        doCallRealMethod().when(toReturn).doGet();
        doCallRealMethod().when(toReturn).doOptions();
        return toReturn;
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
}
