package jiaoni.common.appengine.access.sms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jiaoni.common.utils.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static jiaoni.common.appengine.access.sms.SmsUtils.NATION_CODE_US;

public class TwilioSmsClient implements SmsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioSmsClient.class);

    private final Profile profile;

    public TwilioSmsClient() {
        this.profile = Secrets.of("twilio.profile.json").getAsJson(Profile.class);
        Twilio.init(profile.accountSid, profile.authToken);
    }

    @Override
    public boolean sendText(final String countryCode,
                            final String targetPhone,
                            final String content) {
        checkArgument(NATION_CODE_US.equals(countryCode));

        Message message = Message.creator(
                new PhoneNumber(targetPhone),
                new PhoneNumber(profile.phone),
                content)
                .create();
        LOGGER.info("Send SMS to {}. Message sid: {}", targetPhone, message.getSid());

        if (Message.Status.FAILED == message.getStatus() || Message.Status.UNDELIVERED == message.getStatus()) {
            LOGGER.error("failed to send message {} to {}-{}. errorCode={}, errorMessage={}. responseMessage={}",
                    message, countryCode, targetPhone, message.getErrorCode(), message.getErrorMessage(), message);
            return false;
        }
        return true;
    }

    @Override
    public boolean sendTextWithTemplate(final String countryCode,
                                        final String targetPhone,
                                        final String templateId,
                                        final String... parameters) {
        throw new UnsupportedOperationException();
    }

    private static class Profile {
        @JsonProperty
        String email;
        @JsonProperty
        String password;
        @JsonProperty
        String phone;
        @JsonProperty
        String accountSid;
        @JsonProperty
        String authToken;
    }
}
