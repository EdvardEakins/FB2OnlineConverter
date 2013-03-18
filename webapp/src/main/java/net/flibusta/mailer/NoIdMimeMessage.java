package net.flibusta.mailer;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class NoIdMimeMessage extends MimeMessage{
    private String messageId;

    public NoIdMimeMessage(Session session) {
        super(session);
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    @Override
    protected void updateMessageID() throws MessagingException {
        if (messageId != null && messageId.length() > 0) {
            setHeader("Message-ID", messageId);
        }
    }
}
