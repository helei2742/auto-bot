package cn.com.helei.bot.core.util;

import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class RestApiClient {

    private static final int RETRY_TIMES = 1;

    @Getter
    private final OkHttpClient okHttpClient;

    private final ExecutorService executorService;

    public RestApiClient(
            ProxyInfo proxy,
            ExecutorService executorService
    ) {
        this.executorService = executorService;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder
                // 连接超时
                .connectTimeout(30, TimeUnit.SECONDS)
                // 读取超时
                .readTimeout(30, TimeUnit.SECONDS)
                // 写入超时
                .writeTimeout(45, TimeUnit.SECONDS);

        if (proxy != null) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, proxy.getAddress()));
            if (StrUtil.isNotBlank(proxy.getUsername())) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
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
            Map<String, String> headers,
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
                requestUrl = url + "?" + queryString;
            }


            Request.Builder builder = new Request.Builder();


            MediaType JSON = MediaType.get("application/json; charset=utf-8");

            RequestBody requestBody = null;
            if (body != null) {
                requestBody = RequestBody.create(body.toJSONString(), JSON);
            }


            // 创建 POST 请求
            builder.url(requestUrl);
            String upperCase = method.toUpperCase();
            if (upperCase.equals("GET")) {
                builder.get();
            } else {
                builder.method(upperCase, requestBody);
            }

            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    builder.addHeader(header.getKey(), header.getValue());
                }
            }
            Request request = builder

                    .build();

            log.debug("创建请求 url[{}], method[{}]成功，开始请求服务器", url, method);

            for (int i = 0; i < RETRY_TIMES; i++) {

                // 发送请求并获取响应
                try (Response response = okHttpClient.newCall(request).execute()) {
                    ResponseBody responseBody = response.body();
                    if (response.isSuccessful()) {
                        try {
                            return responseBody == null ? null : responseBody.string();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {

                        throw new RuntimeException("请求 " + url + "失败, " + (responseBody == null ? null : responseBody.string()));
                    }
                } catch (SocketTimeoutException e) {
                    throw new RuntimeException(String.format("请求[%s]超时，尝试重新请求 [%s/%s],", url, i, RETRY_TIMES), e);
                } catch (IOException e) {
                    throw new RuntimeException("请求url [" + url + "] 失败", e);
                }
            }

            return null;
        }, executorService);
    }
}
