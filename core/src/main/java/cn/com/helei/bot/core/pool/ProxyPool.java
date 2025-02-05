package cn.com.helei.bot.core.pool;

import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.mvc.service.IProxyInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Component
public class ProxyPool extends MemoryPool<ProxyInfo> {

    @Autowired
    private IProxyInfoService proxyInfoService;

    public ProxyPool(
            IProxyInfoService proxyInfoService
    ) {
        super(ProxyInfo.class);
    }

    @Override
    protected void fillPool(ConcurrentMap<Integer, ProxyInfo> idMapItem) {
        List<ProxyInfo> all = proxyInfoService.query().list();

        all.forEach(proxyInfo -> {
            idMapItem.put(proxyInfo.getId(), proxyInfo);
        });
    }
}
