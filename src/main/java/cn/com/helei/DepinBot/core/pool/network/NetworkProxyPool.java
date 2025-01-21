package cn.com.helei.DepinBot.core.pool.network;

import cn.com.helei.DepinBot.core.pool.AbstractYamlLinePool;


public class NetworkProxyPool extends AbstractYamlLinePool<NetworkProxy> {

    public NetworkProxyPool() {
        super(NetworkProxy.class);
    }


    public static NetworkProxyPool getDefault() {
        return loadYamlPool(
                "bot/network-proxy.yaml",
                "bot.network.proxy",
                NetworkProxyPool.class
        );
    }

    public static void main(String[] args) {
        System.out.println(getDefault());
    }
}
