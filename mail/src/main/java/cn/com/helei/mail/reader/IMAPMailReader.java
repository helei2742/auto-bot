package cn.com.helei.mail.reader;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class IMAPMailReader {

    public static void main(String[] args) {
        String host = "imap.mailxw.com"; // IMAP服务器地址
        String username = "tqgaerniml@mailxw.com";
        String password = "123456789helei"; // 应用专用密码或授权码

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.ssl.enable", "true");

        try {
            // 创建会话
            Session session = Session.getInstance(properties);
            // 连接到邮件存储
            Store store = session.getStore("imap");
            store.connect(username, password);

            // 打开收件箱
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // 获取邮件
            Message[] messages = inbox.getMessages();
            System.out.println("总邮件数: " + messages.length);

            for (int i = 0; i < Math.min(10, messages.length); i++) {
                Message message = messages[i];
                System.out.println("邮件 " + (i + 1) + " 主题: " + message.getSubject());
                System.out.println("发件人: " + message.getFrom()[0]);
                System.out.println("内容: " + getTextFromMessage(message));
            }

            // 关闭连接
            inbox.close(false);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 解析邮件内容
    private static String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                result.append(bodyPart.getContent().toString());
            }
            return result.toString();
        }
        return "";
    }
}
