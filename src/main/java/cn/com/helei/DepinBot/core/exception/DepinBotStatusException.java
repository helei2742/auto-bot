package cn.com.helei.DepinBot.core.exception;

public class DepinBotStatusException extends RuntimeException{

    // 默认构造函数
    public DepinBotStatusException() {
        super("DepinBotStatus error.");
    }

    // 传入错误信息的构造函数
    public DepinBotStatusException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public DepinBotStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public DepinBotStatusException(Throwable cause) {
        super(cause);
    }
}
