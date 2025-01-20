package cn.com.helei.DepinBot.core.util;

import cn.com.helei.DepinBot.core.network.NetworkProxy;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


@Slf4j
public class RestApiClient {

    private static final int RETRY_TIMES = 3;

    private final OkHttpClient okHttpClient;

    private final ExecutorService executorService;

    public RestApiClient(
            NetworkProxy proxy,
            ExecutorService executorService
    ) {
        this.executorService = executorService;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (proxy != null) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, proxy.getAddress()))
                    .proxyAuthenticator((route, response) -> {
                        String credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    })
            ;
        }
        this.okHttpClient = builder.build();
    }


    /**
     * 发送请求，如果有asKey参数不为null，则会鉴权
     *
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<JSONObject>
     */
    public CompletableFuture<String> request(
            String url,
            String method,
            HttpHeaders headers,
            JSONObject params,
            JSONObject body
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // 创建表单数据
            StringBuilder queryString = new StringBuilder();

            String requestUrl = url;
            if (params != null) {
                params.keySet().forEach(key -> {
                    queryString.append(key).append("=").append(params.get(key)).append("&");
                });

                if (!queryString.isEmpty()) {
                    queryString.deleteCharAt(queryString.length() - 1);
                }
                requestUrl = url + "?" + queryString;;
            }


            FormBody.Builder bodyBuilder = new FormBody.Builder();

            if (body != null) {
                body.forEach((k, v) -> bodyBuilder.add(k, String.valueOf(v)));
            }

            Request.Builder builder = new Request.Builder();

            if (headers != null) {
                for (Map.Entry<String, String> header : headers) {
                    builder.addHeader(header.getKey(), header.getValue());
                }
            }


            // 创建 POST 请求
            builder.url(requestUrl);
            String upperCase = method.toUpperCase();
            if (upperCase.equals("GET")) {
                builder.get();
            } else {
                builder.method(upperCase, bodyBuilder.build());
            }

            Request request = builder.build();

            log.debug("创建请求 url[{}], method[{}]成功，开始请求服务器", url, method);

            for (int i = 0; i < RETRY_TIMES; i++) {
                // 发送请求并获取响应
                try (Response response = okHttpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body() == null ? "{}" : response.body().string();
                    } else {
                        log.error("请求url [{}] 失败， code [{}]， {}",
                                url, response.code(), response.body() != null ? response.body().string() : null);
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    log.warn("请求[{}]超时，尝试重新请求 [{}/{}],", url, i, RETRY_TIMES, e);
                } catch (IOException e) {
                    log.error("请求url [{}] 失败", url, e);
                    throw new RuntimeException(e);
                }
            }

            return null;
        }, executorService);
    }
}
