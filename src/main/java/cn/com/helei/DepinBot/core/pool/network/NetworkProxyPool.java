package cn.com.helei.DepinBot.core.pool.network;

import cn.com.helei.DepinBot.core.pool.AbstractYamlLineItem;
import cn.com.helei.DepinBot.core.pool.AbstractYamlLinePool;
import cn.com.helei.DepinBot.core.util.table.CommandLineTablePrintHelper;

import java.util.ArrayList;
import java.util.Collection;


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
        System.out.println(getDefault().printPool());
    }
}
