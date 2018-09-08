package jiaonidaigou.appengine.api.access.sms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jiaonidaigou.appengine.common.utils.Secrets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static jiaonidaigou.appengine.api.access.sms.SmsUtils.NATION_CODE_CN;

@Singleton
public class QCloudSmsClient implements SmsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(QCloudSmsClient.class);

    private final SmsSingleSender sender;

    @Inject
    public QCloudSmsClient() {
        Profile profile = Secrets.of("qcloud.sms.profile.json").getAsJson(Profile.class);
        this.sender = new SmsSingleSender(profile.appId, profile.appKey);
    }

    @Override
    public boolean sendText(final String countryCode,
                            final String targetPhone,
                            final String content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean sendTextWithTemplate(final String countryCode,
                                        final String targetPhone,
                                        final String templateId,
                                        final String... parameters) {
        checkArgument(NATION_CODE_CN.equals(countryCode));
        SmsSingleSenderResult result = null;
        try {
            result = sender.sendWithParam(
                    NATION_CODE_CN,
                    targetPhone,
                    Integer.parseInt(templateId),
                    parameters,
                    "",
                    "",
                    "");
        } catch (Exception e) {
            LOGGER.error("Send sms failed. templateId {}, parameters {}",
                    templateId, Arrays.toString(parameters), e);
            return false;
        }
        if (StringUtils.isNotBlank(result.errMsg)) {
            LOGGER.error("Send sms failed. templateId {}, parameters {}, response {}, response.err: {}",
                    templateId, Arrays.toString(parameters), result, result.errMsg);
            return false;
        }
        return true;
    }

    private static class Profile {
        @JsonProperty
        int appId;
        @JsonProperty
        String appKey;
    }
}
