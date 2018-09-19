package jiaonidaigou.appengine.api.access.email;

import com.google.common.base.Charsets;
import jiaonidaigou.appengine.common.utils.Environments;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class FakePopupEmailSender implements EmailClient {
    @Override
    public void sendHtml(final String to,
                         final String subject,
                         final String htmlContent) {
        send(to, subject, htmlContent);
    }

    @Override
    public void sendText(final String to,
                         final String subject,
                         final String text) {
        String formattedText = StringUtils.replace(text, "\n", "<br />");
        formattedText = StringUtils.replace(formattedText, "\t", "&#09;");
        send(to, subject, formattedText);
    }

    private void send(final String to,
                      final String subject,
                      final String text) {
        String file = Environments.LOCAL_TEMP_DIR_ENDSLASH + "fake_email.html";
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8))) {
            writer.write("[" + to + "]" + subject + "\n");
            writer.write(text);
            Runtime.getRuntime().exec("open " + file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
