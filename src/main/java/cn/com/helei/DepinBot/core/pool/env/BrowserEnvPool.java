package cn.com.helei.DepinBot.core.pool.env;


import cn.com.helei.DepinBot.core.pool.AbstractYamlLinePool;

public class BrowserEnvPool extends AbstractYamlLinePool<BrowserEnv> {

    public BrowserEnvPool() {
        super(BrowserEnv.class);
    }

    public static BrowserEnvPool getDefault() {
        return loadYamlPool(
                "bot/browser-env.yaml",
                "bot.network.proxy.list",
                BrowserEnvPool.class
        );
    }
}
