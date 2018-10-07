package jiaoni.common.appengine.access.email;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import javax.inject.Singleton;
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

public class LocalGmailSender implements EmailClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalGmailSender.class);
    private static final EmailValidator emailValidator = EmailValidator.getInstance();

    private final String senderGmail;
    private final String senderGmailPassword;

    public LocalGmailSender(String senderGmail, String senderGmailPassword) {
        checkArgument(StringUtils.endsWithIgnoreCase(senderGmail, "@gmail.com"));
        checkArgument(emailValidator.isValid(senderGmail));
        checkArgument(StringUtils.isNotBlank(senderGmailPassword));
        this.senderGmail = senderGmail;
        this.senderGmailPassword = senderGmailPassword;
    }

    @Override
    public void sendHtml(String to, String subject, String htmlContent) {
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
    public void sendText(String to, String subject, String text) {
        checkArgument(emailValidator.isValid(to));
        BodyPart bodyPart = new MimeBodyPart();
        try {
            bodyPart.setText(text);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        send(to, subject, bodyPart);
    }

    private void send(final String to, final String subject, final BodyPart bodyPart) {
        Properties props = System.getProperties();
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.user", senderGmail);
        props.put("mail.smtp.password", senderGmailPassword);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", true);
        props.put("mail.mime.charset", "utf-8");

        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setSubject(subject);
            message.setFrom(new InternetAddress(senderGmail));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            Multipart multipart = new MimeMultipart("alternative");

            // Create your text message part
            multipart.addBodyPart(bodyPart);
            message.setContent(multipart);

            // Send message
            Transport transport = session.getTransport("smtps");
            transport.connect("smtp.gmail.com", senderGmail, senderGmailPassword);

            LOGGER.info("Connect transport: {}", transport.toString());

            transport.sendMessage(message, message.getAllRecipients());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
