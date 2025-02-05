package cn.com.helei.bot.core.util.exception;

public class DepinBotInitException extends Exception{

    // 默认构造函数
    public DepinBotInitException() {
        super("Account initialization failed.");
    }

    // 传入错误信息的构造函数
    public DepinBotInitException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public DepinBotInitException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public DepinBotInitException(Throwable cause) {
        super(cause);
    }
}
