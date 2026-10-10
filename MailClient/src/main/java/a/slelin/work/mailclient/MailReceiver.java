package a.slelin.work.mailclient;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class MailReceiver {

    private final ConfigManager config;

    public MailReceiver(ConfigManager config) {
        this.config = config;
    }

    public List<MailInfo> fetchLast(int count) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.connectiontimeout", "10000");
        props.put("mail.imap.timeout", "10000");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        List<MailInfo> result = new ArrayList<>();

        try {
            store.connect(config.getImapHost(), config.getImapPort(), config.getUser(), config.getPassword());
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int total = inbox.getMessageCount();
            if (total == 0) {
                inbox.close(false);
                return result;
            }

            int start = Math.max(1, total - count + 1);
            Message[] messages = inbox.getMessages(start, total);

            SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm");

            for (Message m : messages) {
                MimeMessage mm = (MimeMessage) m;
                String from = (mm.getFrom() != null && mm.getFrom().length > 0)
                        ? mm.getFrom()[0].toString() : "(неизвестно)";
                String subject = mm.getSubject() != null ? mm.getSubject() : "(без темы)";
                Date sentDate = mm.getSentDate() != null ? mm.getSentDate() : mm.getReceivedDate();
                String dateStr = sentDate != null ? fmt.format(sentDate) : "-";
                String preview = extractPreview(mm);
                result.add(new MailInfo(from, subject, dateStr, preview));
            }

            Collections.reverse(result);
            inbox.close(false);
        } finally {
            if (store.isConnected()) {
                store.close();
            }
        }

        return result;
    }

    private String extractPreview(Message message) {
        try {
            Object content = message.getContent();
            String text;
            if (content instanceof String s) {
                text = s;
            } else if (content instanceof Multipart mp) {
                text = extractFromMultipart(mp);
            } else {
                text = "";
            }
            text = text.replaceAll("\\s+", " ").trim();
            return text.length() > 120 ? text.substring(0, 120) + "..." : text;
        } catch (Exception e) {
            return "";
        }
    }

    private String extractFromMultipart(Multipart mp) throws MessagingException, IOException {
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart bp = mp.getBodyPart(i);
            if (bp.isMimeType("text/plain")) {
                Object c = bp.getContent();
                if (c instanceof String s) return s;
            }
        }
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart bp = mp.getBodyPart(i);
            Object c = bp.getContent();
            if (c instanceof Multipart nested) {
                String r = extractFromMultipart(nested);
                if (!r.isBlank()) return r;
            }
        }
        return "";
    }

    public record MailInfo(String from, String subject, String date, String preview) {
    }
}