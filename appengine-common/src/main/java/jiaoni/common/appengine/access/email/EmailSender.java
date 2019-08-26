package jiaoni.common.appengine.access.email;

import java.util.List;

public interface EmailSender {
    void sendHtml(final String to,
                  final String subject,
                  final String htmlContent);

    void sendText(final String to,
                  final String subject,
                  final String text);

    default void sendText(final List<String> tos,
                          final String subject,
                          final String text) {
        for (String to : tos) {
            sendText(to, subject, text);
        }
    }
}
