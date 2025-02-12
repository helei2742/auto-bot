package cn.com.helei.bot.core.util;

import cn.com.helei.bot.core.pool.network.NetworkProxy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class RestApiClientFactory {

    private static final ConcurrentHashMap<NetworkProxy, RestApiClient> CLIENTS = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newThreadPerTaskExecutor(new NamedThreadFactory("rest-api-client"));

    public static final RestApiClient DEFAULT = new RestApiClient(null, executor);

    public static RestApiClient getClient(NetworkProxy proxy) {
        if (proxy == null) {
            return DEFAULT;
        }

        return CLIENTS.compute(proxy, (k, v)->{
            if (v == null) {
                v = new RestApiClient(proxy, executor);
            }
            return v;
        });
    }
}
