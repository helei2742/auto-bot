package cn.com.helei.bot.core.util.exception;

public class RequestException extends Exception{

    // 默认构造函数
    public RequestException() {
        super("request failed.");
    }

    // 传入错误信息的构造函数
    public RequestException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public RequestException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public RequestException(Throwable cause) {
        super(cause);
    }
}
