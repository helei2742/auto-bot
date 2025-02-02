package cn.com.helei.bot.core.supporter.mail.exception;

public class MailReadException extends RuntimeException{

    // 默认构造函数
    public MailReadException() {
        super("read mail failed.");
    }

    // 传入错误信息的构造函数
    public MailReadException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public MailReadException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public MailReadException(Throwable cause) {
        super(cause);
    }
}
