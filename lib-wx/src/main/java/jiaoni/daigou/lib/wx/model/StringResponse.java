package jiaoni.daigou.lib.wx.model;

import com.google.common.primitives.Ints;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class StringResponse {
    private final Map<String, String> responseMap = new HashMap<>();
    private final String rawResponse;

    private StringResponse(final String rawResponse) {
        this.rawResponse = rawResponse;
    }

    /**
     * The response looks like:
     * window.QRLogin.code = 200; window.QRLogin.uuid = "xxx"
     */
    public static StringResponse responseOf(final String rawResponse) {
        StringResponse toReturn = new StringResponse(rawResponse);
        if (StringUtils.isBlank(rawResponse)) {
            return toReturn;
        }
        String[] parts = StringUtils.split(rawResponse, ";");
        for (String part : parts) {
            String name = trimToNull(substringBefore(part, "="));
            String value = trimToNull(substringAfter(part, "="));
            if (name == null || value == null) {
                continue;
            }
            toReturn.responseMap.put(name, value);
        }
        return toReturn;
    }

    public String getRaw(final String prop) {
        return responseMap.get(prop);
    }

    public String getAsString(final String prop) {
        String raw = responseMap.get(prop);
        if (raw == null || raw.length() < 2 || (!raw.startsWith("\"")) || (!raw.endsWith("\""))) {
            return null;
        }
        return raw.substring(1, raw.length() - 1);
    }

    public int getAsInteger(final String prop, int defaultVal) {
        String raw = responseMap.get(prop);
        Integer toReturn = raw == null ? null : Ints.tryParse(raw);
        return toReturn == null ? defaultVal : toReturn;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
