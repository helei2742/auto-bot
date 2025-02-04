package cn.com.helei.bot.core.supporter.mail.factory;

import org.junit.jupiter.api.Test;


import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MailReaderFactoryTest {


    public static void main(String[] args) throws Exception {
        String host = "imap.gmail.com";
        String username = "getejanacworda@gmail.com";  // 替换为你的 Gmail 地址
        String password = "mbwsgrkovvzfnvoy";    // 使用应用专用密码，不能用 Gmail 登录密码

        // 设置 IMAP 配置
        var properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.ssl.enable", "true");

        // 获取会话对象
        Session session = Session.getDefaultInstance(properties);

        // 连接到邮箱服务器
        Store store = session.getStore("imaps");
        store.connect(host, username, password);

        // 获取收件箱
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        // 获取邮件
        Message[] messages = inbox.getMessages();

        System.out.println("You have " + messages.length + " messages in your inbox:");

        // 遍历邮件并打印主题
        for (Message message : messages) {
            System.out.println("Subject: " + message.getSubject());
        }

        // 关闭文件夹和连接
        inbox.close(false);
        store.close();
    }
}
