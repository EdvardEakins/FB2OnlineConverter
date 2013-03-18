package net.flibusta.mailer;

import org.apache.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class BookSender implements Runnable {

    Logger logger = Logger.getLogger(BookSender.class);


    private String bookId;
    private File book;
    private String format;
    private String targetAddress;
    private Properties mailSessionProperties;
    private String fromAddress;
    private String fromName;

    public BookSender(File book, String bookId, String format, String targetAddress, Properties mailSessionProperties, String fromAddress, String fromName) {
        this.book = book;
        this.bookId = bookId;
        this.format = format;
        this.targetAddress = targetAddress;
        this.mailSessionProperties = mailSessionProperties;

//        properties = new Properties();
//        properties.put("mail.smtp.user", fromAddress);
////        properties.put("mail.smtp.port", 465);
//        properties.put("mail.smtp.port", 587);
//        properties.put("mail.smtp.host", "109.163.230.117");
////        properties.put("mail.smtp.host", "smtp.gmail.com");
//
//        properties.put("mail.smtp.auth", "false");
////        properties.put("mail.smtp.auth", "true");
////        properties.put("mail.smtp.starttls.enable", "true");
////        properties.put("mail.smtp.socketFactory.port", "465");
////        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
////        properties.put("mail.smtp.socketFactory.fallback", "false");
////            properties.put("mail.smtp.debug", "true");

        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    public void run() {
        File fileAttachment = book;
        try {
//                Authenticator auth = new SMTPAuthenticator(fromAddress, fromPassword);
//                Session session = Session.getInstance(properties, auth);
            Session session = Session.getInstance(mailSessionProperties);

            NoIdMimeMessage message = new NoIdMimeMessage(session);
            message.setMessageId("book." + bookId);
            message.addFrom(new Address[]{new InternetAddress(fromAddress, fromName)});
            message.addRecipients(Message.RecipientType.TO, new Address[]{new InternetAddress(targetAddress)});


            message.setSubject("Requested book" + fileAttachment.getName());

            Multipart multipart = new MimeMultipart();

            // create the message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            //fill message
            messageBodyPart.setText("book " + fileAttachment.getName());

            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(fileAttachment); // @todo content type
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(fileAttachment.getName());
            multipart.addBodyPart(messageBodyPart);

            // Put parts in message
            message.setContent(multipart);

            Transport.send(message);
        } catch (MessagingException e) {
            logger.error("Can't send mail to user for document " + bookId, e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Can't send mail to user for document " + bookId, e);
            throw new RuntimeException(e.getMessage(), e);
        }

    }
}
