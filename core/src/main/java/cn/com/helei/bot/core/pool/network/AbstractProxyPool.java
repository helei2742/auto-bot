package cn.com.helei.bot.core.pool.network;

import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
import cn.com.helei.bot.core.pool.AbstractYamlLinePool;
import cn.com.helei.bot.core.util.RestApiClient;
import cn.com.helei.bot.core.util.RestApiClientFactory;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
public abstract class AbstractProxyPool extends AbstractYamlLinePool<NetworkProxy> {

    public static final NetworkProxy DEFAULT_PROXY = new NetworkProxy();

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


    public static void checkProxyUsable(AbstractProxyPool proxyPool, int concurrentCount) throws InterruptedException {
        log.info("开始检查[{}]代理是否可用。。。", proxyPool.getProxyType());

        Semaphore semaphore =
                new Semaphore(concurrentCount);

        String url = "https://ipwhois.app/json";

        AtomicInteger usableCount = new AtomicInteger(0);
        AtomicInteger completedCount = new AtomicInteger(0);

        List<CompletableFuture<String>> futures = new ArrayList<>();
        List<NetworkProxy> proxyList = proxyPool.getAllItem();
        for (NetworkProxy networkProxy : proxyList) {
            semaphore.acquire();

            CompletableFuture<String> future = RestApiClientFactory
                    .getClient(null)
                    .request(
                            url + "/" + networkProxy.getAddressStr(),
                            "get",
                            null,
                            null,
                            null
                    )
                    .whenCompleteAsync((response, throwable) -> {
                        semaphore.release();
                        int complete = completedCount.incrementAndGet();
                        if (throwable != null) {
                            log.warn("{}-代理[{}]不可用, [{}/{}]",
                                    networkProxy.getProxyType(), networkProxy.getAddressStr(), complete, proxyList.size());
                            networkProxy.setUsable(false);
                        } else {
                            log.info("{}-代理[{}]可用,[{}/{}]",
                                    networkProxy.getProxyType(), networkProxy.getAddressStr(), complete, proxyList.size());
                            usableCount.incrementAndGet();
                            networkProxy.setUsable(true);

                            JSONObject metaInfo = JSONObject.parseObject(response);
                            networkProxy.setMetadata(metaInfo);
                        }
                    });
            futures.add(future);
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.debug("error", e);
        }

        log.info("[{}]代理检查完毕, [{}/{}](可用/不可用)", proxyPool.getProxyType(),
                usableCount.get(), proxyList.size());
    }
}

