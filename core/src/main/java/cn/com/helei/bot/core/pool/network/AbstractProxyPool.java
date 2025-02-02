package cn.com.helei.bot.core.pool.network;

import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
import cn.com.helei.bot.core.pool.AbstractYamlLinePool;

public abstract class AbstractProxyPool extends AbstractYamlLinePool<NetworkProxy> {

    private final ProxyType proxyType;

    public AbstractProxyPool(ProxyType proxyType) {
        super(NetworkProxy.class);
        this.proxyType = proxyType;
    }

    @Override
    protected void itemCreatedHandler(AbstractYamlLineItem item) {
        NetworkProxy proxy = (NetworkProxy) item;
        proxy.setProxyType(proxyType);
    }
}

