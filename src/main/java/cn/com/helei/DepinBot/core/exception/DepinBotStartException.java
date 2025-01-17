package cn.com.helei.DepinBot.core.exception;

public class DepinBotStartException extends Exception{

    // 默认构造函数
    public DepinBotStartException() {
        super("Bot start failed.");
    }

    // 传入错误信息的构造函数
    public DepinBotStartException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public DepinBotStartException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public DepinBotStartException(Throwable cause) {
        super(cause);
    }
}
