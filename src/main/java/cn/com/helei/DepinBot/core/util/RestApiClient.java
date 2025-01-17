package cn.com.helei.DepinBot.core.util;

import cn.com.helei.DepinBot.core.network.NetworkProxy;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
        import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
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
                    .authenticator(new Authenticator() {
                        @NotNull
                        @Override
                        public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
                            String credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                            return response.request().newBuilder()
                                    .header("Authorization", credential)
                                    .build();
                        }
                    });
        }
        this.okHttpClient = builder.build();
    }


    /**
     * 发送请求，如果有asKey参数不为null，则会鉴权
     *
     * @param method method
     * @param params params
     * @param body   body
     * @return CompletableFuture<JSONObject>
     */
    public CompletableFuture<String> request(
            String url,
            String method,
            JSONObject params,
            JSONObject body
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // 创建表单数据
            StringBuilder queryString = new StringBuilder();


            if (params != null) {
                params.keySet().forEach(key -> {
                    queryString.append(key).append("=").append(params.get(key)).append("&");
                });

                if (!queryString.isEmpty()) {
                    queryString.deleteCharAt(queryString.length() - 1);
                }
            }

            String requestUrl = url + "?" + queryString;
            FormBody.Builder bodyBuilder = new FormBody.Builder();

            if (body != null) {
                body.forEach((k, v) -> bodyBuilder.add(k, String.valueOf(v)));
            }

            Request.Builder builder = new Request.Builder();
            builder.header("Content-Type", "application/json");

            // 创建 POST 请求
            builder.url(requestUrl);
            String upperCase = method.toUpperCase();
            if (upperCase.equals("GET")) {
                builder.get();
            } else {
                builder.method(upperCase, bodyBuilder.build());
            }

            Request request = builder.build();

            log.info("创建请求 url[{}], method[{}]成功，开始请求服务器", url, method);

            for (int i = 0; i < RETRY_TIMES; i++) {
                // 发送请求并获取响应
                try (Response response = okHttpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body() == null ? "{}" : response.body().string();
                    } else {
                        log.error("请求url [{}] 失败， code [{}]， {}", url, response.code(), response.body());
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    log.warn("请求[{}]超时，尝试重新请求 [{}/{}]", url, i, RETRY_TIMES);
                } catch (IOException e) {
                    log.error("请求url [{}] 失败", url, e);
                    throw new RuntimeException(e);
                }
            }

            return null;
        }, executorService);
    }
}
