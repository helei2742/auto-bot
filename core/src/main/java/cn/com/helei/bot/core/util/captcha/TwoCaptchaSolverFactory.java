package cn.com.helei.bot.core.util.captcha;

import cn.com.helei.bot.core.pool.network.NetworkProxy;
import com.twocaptcha.TwoCaptcha;

public class TwoCaptchaSolverFactory {

    public static TwoCaptcha getTwoCaptchaSolver(String apiKey, NetworkProxy proxy) {
        TwoCaptcha solver = new TwoCaptcha(apiKey);
        solver.setHttpClient(new ProxyApiClient(proxy));
        return solver;
    }
}
