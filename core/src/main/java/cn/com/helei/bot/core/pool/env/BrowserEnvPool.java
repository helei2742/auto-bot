package cn.com.helei.bot.core.pool.env;


import cn.com.helei.bot.core.pool.AbstractYamlLinePool;
import cn.com.helei.bot.core.pool.network.NetworkProxyPool;

public class BrowserEnvPool extends AbstractYamlLinePool<BrowserEnv> {

    public BrowserEnvPool() {
        super(BrowserEnv.class);
    }

    public static BrowserEnvPool getDefault() {
        return loadYamlPool(
                "bot/browser-env.yaml",
                "bot.browser.env",
                BrowserEnvPool.class
        );
    }


    public static void main(String[] args) {
        System.out.println(NetworkProxyPool.getDefault());
    }
}
