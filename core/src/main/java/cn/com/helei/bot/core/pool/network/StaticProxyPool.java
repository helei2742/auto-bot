package cn.com.helei.bot.core.pool.network;

import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
import cn.com.helei.bot.core.pool.AbstractYamlLinePool;


public class StaticProxyPool extends AbstractYamlLinePool<NetworkProxy> {

    public StaticProxyPool() {
        super(NetworkProxy.class);
    }


    public static StaticProxyPool getDefault() {
        return loadYamlPool(
                "bot/proxy-static.yaml",
                "bot.network.proxy-static",
                StaticProxyPool.class
        );
    }

    @Override
    protected void itemCreatedHandler(AbstractYamlLineItem item) {
        NetworkProxy proxy = (NetworkProxy) item;
        proxy.setProxyType(ProxyType.STATIC);
    }

    public static void main(String[] args) {
        System.out.println(getDefault().printPool());
    }
}
