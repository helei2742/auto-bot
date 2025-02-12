package cn.com.helei.bot.core.util.captcha;

public class CaptchaResolveException extends Exception{

    // 默认构造函数
    public CaptchaResolveException() {
        super("captcha resolve failed");
    }

    // 传入错误信息的构造函数
    public CaptchaResolveException(String message) {
        super(message);
    }

    // 传入错误信息和异常原因的构造函数
    public CaptchaResolveException(String message, Throwable cause) {
        super(message, cause);
    }

    // 传入异常原因的构造函数
    public CaptchaResolveException(Throwable cause) {
        super(cause);
    }
}
