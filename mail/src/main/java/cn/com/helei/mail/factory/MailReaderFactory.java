package cn.com.helei.mail.factory;

import cn.com.helei.mail.constants.MailProtocolType;
import cn.com.helei.mail.reader.MailReader;


public class MailReaderFactory {

    public static MailReader getMailReader(MailProtocolType protocol, String host, String port, boolean useSSL) {
        return new MailReader(protocol.name(), host, port, String.valueOf(useSSL));
    }

}
