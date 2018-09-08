package jiaonidaigou.appengine.api.access.sms;

public interface SmsClient {

    /**
     * Send text message.
     */
    boolean sendText(final String countryCode,
                     final String targetPhone,
                     final String content);

    /**
     * Send text message by template.
     */
    boolean sendTextWithTemplate(final String countryCode,
                                 final String targetPhone,
                                 final String templateId,
                                 final String... parameters);
}
