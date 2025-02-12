package cn.com.helei.bot.core.util;

import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
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
                .connectTimeout(25, TimeUnit.SECONDS)
                // 读取超时
                .readTimeout(120, TimeUnit.SECONDS)
                // 写入超时
                .writeTimeout(60, TimeUnit.SECONDS);

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
            Request request = buildRequest(url, method, headers, params, body);

            return normalRequest(url, method, request);
        }, executorService);
    }

    public CompletableFuture<List<String>> streamRequest(
            String url,
            String method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Request request = buildRequest(url, method, headers, params, body);


            Exception exception = null;
            for (int i = 0; i < RETRY_TIMES; i++) {
                // 发送请求并获取响应
                try (Response response = okHttpClient.newCall(request).execute()) {
                    return streamRequest(url, response);
                } catch (SocketTimeoutException e) {
                    log.warn("请求[{}]超时，尝试重新请求 [{}}/{}],", url, i, RETRY_TIMES);
                    exception = e;
                } catch (IOException e) {
                    throw new RuntimeException("请求url [" + url + "] 失败", e);
                }
            }

            throw new RuntimeException("请求重试次数超过限制, " + RETRY_TIMES, exception);
        });
    }

    @NotNull
    private static Request buildRequest(String url, String method, Map<String, String> headers, JSONObject params, JSONObject body) {
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
        return builder.build();
    }


    @NotNull
    private static List<String> streamRequest(String url, Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (response.isSuccessful() || responseBody == null) {
            throw new RuntimeException("请求 " + url + "失败, " + (responseBody == null ? null : responseBody.string()));
        }

        List<String> result = new ArrayList<>();

        BufferedSource source = responseBody.source();
        while (!source.exhausted()) {
            String chunk = source.readUtf8Line();
            System.out.println(chunk);
            if (chunk != null) {
                result.add(chunk);
            }
        }
        return result;
    }


    @Nullable
    private String normalRequest(String url, String method, Request request) {
        log.debug("创建请求 url[{}], method[{}]成功，开始请求服务器", url, method);

        Exception exception = null;
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
                    String body = "";
                    if (responseBody != null) {
                        body =  responseBody.string();
                        body = body.substring(0, Math.min(body.length(), 200));
                    }
                    throw new RuntimeException("请求 " + url + "失败[" + response.code() + "], "+ body);
                }
            } catch (SocketTimeoutException e) {
                log.warn("请求[{}]超时，尝试重新请求 [{}}/{}],", url, i, RETRY_TIMES);
                exception = e;
            } catch (IOException e) {
                throw new RuntimeException("请求url [" + url + "] 失败", e);
            }
        }

        throw new RuntimeException("请求重试次数超过限制, " + RETRY_TIMES, exception);
    }
}
