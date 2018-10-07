package jiaoni.common.appengine.access.email;

import jiaoni.common.utils.Envs;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import static com.google.common.base.Preconditions.checkArgument;

public class GaeEmailSender implements EmailClient {
    private static final EmailValidator emailValidator = EmailValidator.getInstance();

    @Override
    public void sendHtml(final String to, final String subject, final String htmlContent) {
        checkArgument(emailValidator.isValid(to));
        BodyPart bodyPart = new MimeBodyPart();
        try {
            bodyPart.setContent(htmlContent, "text/html; charset=utf-8");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        send(to, subject, bodyPart);
    }

    @Override
    public void sendText(final String to, final String subject, final String text) {
        checkArgument(emailValidator.isValid(to));
        BodyPart bodyPart = new MimeBodyPart();
        try {
            bodyPart.setText(text);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        send(to, subject, bodyPart);
    }

    private void send(String to, String subject, BodyPart bodyPart) {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        try {
            Message message = new MimeMessage(session);
            message.setSubject(subject);
            message.setFrom(new InternetAddress(Envs.getGaeAdminEmail()));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(bodyPart);
            message.setContent(multipart);

            Transport.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
