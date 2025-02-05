package cn.com.helei.bot.core.util.exception;

public class RewardQueryException extends RuntimeException{

    // 默认构造函数
    public RewardQueryException() {
        super("Account reward query failed.");
    }

    // 传入错误信息的构造函数
    public RewardQueryException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public RewardQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public RewardQueryException(Throwable cause) {
        super(cause);
    }
}
