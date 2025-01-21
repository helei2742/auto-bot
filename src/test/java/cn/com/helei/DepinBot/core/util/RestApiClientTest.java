package cn.com.helei.DepinBot.core.util;

import cn.com.helei.DepinBot.core.pool.network.NetworkProxyPool;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

class RestApiClientTest {

    @Test
    public void testRequest() throws ExecutionException, InterruptedException {
        NetworkProxyPool networkProxyPool = NetworkProxyPool.getDefault();

        RestApiClient restApiClient = new RestApiClient(networkProxyPool.getItem(8), Executors.newVirtualThreadPerTaskExecutor());

        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        CompletableFuture<String> request = restApiClient
                .request("https://www.baidu.com",
                        "get", null, null, null);

        System.out.println(request.get());
    }
}
