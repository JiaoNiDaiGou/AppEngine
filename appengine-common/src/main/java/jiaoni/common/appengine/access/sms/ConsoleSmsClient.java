package jiaoni.common.appengine.access.sms;

import java.util.Arrays;
import javax.inject.Singleton;

@Singleton
public class ConsoleSmsClient implements SmsClient {
    @Override
    public boolean sendText(String countryCode, String targetPhone, String content) {
        System.out.println(String.format("SMS: %s-%s %s", countryCode, targetPhone, content));
        return true;
    }

    @Override
    public boolean sendTextWithTemplate(String countryCode, String targetPhone, String templateId, String... parameters) {
        System.out.println(String.format("SMS: %s-%s tempateId=%s, params=%s",
                countryCode, targetPhone, templateId, Arrays.toString(parameters)));
        return true;
    }
}
