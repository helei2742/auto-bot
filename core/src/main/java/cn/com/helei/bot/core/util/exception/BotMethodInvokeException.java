package cn.com.helei.bot.core.util.exception;

public class BotMethodInvokeException extends RuntimeException{

    // 默认构造函数
    public BotMethodInvokeException() {
        super("invoke bot method failed.");
    }

    // 传入错误信息的构造函数
    public BotMethodInvokeException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public BotMethodInvokeException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public BotMethodInvokeException(Throwable cause) {
        super(cause);
    }
}
