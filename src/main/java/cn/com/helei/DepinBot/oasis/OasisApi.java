package cn.com.helei.DepinBot.oasis;

import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import cn.com.helei.DepinBot.core.util.RestApiClient;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
public class OasisApi {

    private static final Map<NetworkProxy, RestApiClient> proxyRestApiClientMap = new ConcurrentHashMap<>();

    private final ExecutorService executorService;

    public OasisApi(ExecutorService executorService) {
        this.executorService = executorService;
    }


    /**
     * 注册用户
     *
     * @param email      邮箱
     * @param password   密码
     * @param inviteCode 邀请码
     * @return 结果
     */
    public CompletableFuture<Boolean> registerUser(NetworkProxy networkProxy, String email, String password, String inviteCode) {

        JSONObject account = new JSONObject();
        account.put("email", email);
        account.put("password", password);
        account.put("referralCode", inviteCode);

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        return getRestApiClient(networkProxy)
                .request("https://api.oasis.ai/internal/auth/signup", "post", headers, null, account)
                .thenApplyAsync((res) -> {
                    if (Objects.equals(res, "")) {
                        log.info("注册邮箱[{}]成功！请前往验证", email);
                        return true;
                    } else {
                        log.error("注册[{}]失败, {}", email, res);
                        return false;
                    }
                });
    }

    /**
     * 登录用户,返回token
     *
     * @param email    邮箱
     * @param password 密码
     * @return 结果
     */
    public CompletableFuture<String> loginUser(NetworkProxy networkProxy, String email, String password) {

        JSONObject account = new JSONObject();
        account.put("email", email);
        account.put("password", password);
        account.put("rememberSession", true);

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");

        return getRestApiClient(networkProxy)
                .request("https://api.oasis.ai/internal/auth/login", "post", headers, null, account)
                .thenApplyAsync((res) -> {
                    JSONObject result = JSONObject.parseObject(res);
                    return result.getString("token");
                });
    }

    /**
     * 登录用户,返回token
     *
     * @param email    邮箱
     * @return 结果
     */
    public CompletableFuture<Boolean> resendCode(NetworkProxy networkProxy, String email) {

        JSONObject account = new JSONObject();
        account.put("email", email);

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");

        return getRestApiClient(networkProxy)
                .request("https://api.oasis.ai/internal/auth/resend-code", "post", headers, null, account)
                .thenApplyAsync((res) -> {
                    if (StrUtil.isBlank(res)) return true;
                    else {
                        throw new RuntimeException("重发验证码错误，" + res);
                    }
                });
    }


    private RestApiClient getRestApiClient(NetworkProxy networkProxy) {
        if (networkProxy == null) {
            return new RestApiClient(null, executorService);
        }
        return proxyRestApiClientMap.compute(networkProxy, (k, v) -> {
            if (v == null) {
                v = new RestApiClient(networkProxy, executorService);
            }
            return v;
        });
    }

}
