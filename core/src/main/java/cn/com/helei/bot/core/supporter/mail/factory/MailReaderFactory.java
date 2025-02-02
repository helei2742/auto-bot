package cn.com.helei.bot.core.supporter.mail.factory;

import cn.com.helei.bot.core.supporter.mail.constants.MailProtocolType;
import cn.com.helei.bot.core.supporter.mail.reader.MailReader;


public class MailReaderFactory {

    public static MailReader getMailReader(MailProtocolType protocol, String host, String port, boolean useSSL) {
        return new MailReader(protocol.name(), host, port, String.valueOf(useSSL));
    }

}
