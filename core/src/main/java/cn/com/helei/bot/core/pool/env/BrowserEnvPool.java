package cn.com.helei.bot.core.pool.env;


import cn.com.helei.bot.core.pool.AbstractYamlLinePool;
import cn.com.helei.bot.core.pool.network.StaticProxyPool;

public class BrowserEnvPool extends AbstractYamlLinePool<BrowserEnv> {

    public BrowserEnvPool() {
        super(BrowserEnv.class);
    }

}
