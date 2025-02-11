package cn.com.helei.bot.core.util.exception;

public class BotMethodFormatException extends RuntimeException{

    // 默认构造函数
    public BotMethodFormatException() {
        super("error bot method format.");
    }

    // 传入错误信息的构造函数
    public BotMethodFormatException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public BotMethodFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public BotMethodFormatException(Throwable cause) {
        super(cause);
    }
}
