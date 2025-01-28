package cn.com.helei.bot.core.exception;

public class RegisterException extends RuntimeException{

    // 默认构造函数
    public RegisterException() {
        super("Account register failed.");
    }

    // 传入错误信息的构造函数
    public RegisterException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public RegisterException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public RegisterException(Throwable cause) {
        super(cause);
    }
}
