package cn.com.helei.bot.core.pool.network;

import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
import cn.com.helei.bot.core.pool.AbstractYamlLinePool;

public class DynamicProxyPool extends AbstractYamlLinePool<NetworkProxy> {

    public DynamicProxyPool() {
        super(NetworkProxy.class);
    }

    @Override
    protected void itemCreatedHandler(AbstractYamlLineItem item) {
        NetworkProxy proxy = (NetworkProxy) item;
        proxy.setProxyType(ProxyType.DYNAMIC);
    }

}
