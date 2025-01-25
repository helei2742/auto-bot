package cn.com.helei.DepinBot;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class OutlookMailReader {

    public static void main(String[] args) {
        // 配置 Outlook IMAP 服务器

        String host = "outlook.office365.com";
        String username = "ztphfugbtgs77@hotmail.com";
        String password = "YTq3OEXd29";

        Properties props = new Properties();
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", "993");
        props.put("mail.imap.starttls.enable", "true");
        props.put("mail.imap.ssl.enable", "true");

        try {
            // 创建会话
            Session session = Session.getDefaultInstance(props, null);

            // 连接到 IMAP 服务器
            Store store = session.getStore("imap");
            store.connect(host, username, password);

            // 打开收件箱
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // 获取邮件
            Message[] messages = inbox.getMessages();
            System.out.println("Total messages: " + messages.length);

            // 打印每封邮件的主题
            for (Message message : messages) {
                System.out.println("Subject: " + message.getSubject());
            }

            // 关闭资源
            inbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
