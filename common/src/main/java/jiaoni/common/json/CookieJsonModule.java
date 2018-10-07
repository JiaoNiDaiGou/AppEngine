package jiaoni.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie2;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Serialize/Deserialize {@link Cookie} to json.
 */
public class CookieJsonModule extends SimpleModule {
    public CookieJsonModule() {
        addDeserializer(Cookie.class, new CookieDeserializer());
    }

    public static class CookieDeserializer extends JsonDeserializer<Cookie> {
        @Override
        public Cookie deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);

            BasicClientCookie2 cookie = new BasicClientCookie2(
                    node.get("name").textValue(),
                    node.get("value").textValue());

            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String nodeName = entry.getKey();
                JsonNode val = entry.getValue();
                switch (nodeName) {
                    case "name":
                    case "value":
                        // Already handled.
                    case "persistent":
                        // That is a derived field from expiryDate.
                        break;
                    case "creationDate":
                        cookie.setCreationDate(new Date(val.longValue()));
                        break;
                    case "expiryDate":
                        cookie.setExpiryDate(new Date(val.longValue()));
                        break;
                    case "path":
                        cookie.setPath(val.textValue());
                        break;
                    case "domain":
                        cookie.setDomain(val.textValue());
                        break;
                    case "comment":
                        cookie.setComment(val.textValue());
                        break;
                    case "commentURL":
                        cookie.setCommentURL(val.textValue());
                        break;
                    case "ports":
                        if (val.isArray()) {
                            int[] ports = new int[val.size()];
                            for (int i = 0; i < ports.length; i++) {
                                ports[i] = val.get(i).intValue();
                            }
                            cookie.setPorts(ports);
                        }
                        break;
                    case "version":
                        cookie.setVersion(val.intValue());
                        break;
                    case "secure":
                        cookie.setSecure(val.booleanValue());
                        break;
                    default:
                        cookie.setAttribute(nodeName, val.asText());
                        break;
                }
            }
            return cookie;
        }
    }
}
