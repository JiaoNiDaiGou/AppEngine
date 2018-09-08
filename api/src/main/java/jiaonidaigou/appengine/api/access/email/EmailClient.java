package jiaonidaigou.appengine.api.access.email;

public interface EmailClient {
    void sendHtml(final String to,
                  final String subject,
                  final String htmlContent);

    void sendText(final String to,
                  final String subject,
                  final String text);
}
