package cn.com.helei.application.openloop;

import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.LoginException;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OpenLoopApi {

    private static final Random random = new Random();

    private final OpenLoopAutoBot openLoopDepinBot;

    public OpenLoopApi(OpenLoopAutoBot openLoopDepinBot) {
        this.openLoopDepinBot = openLoopDepinBot;
    }


    /**
     * 注册用户
     *
     * @param accountContext accountContext
     * @param inviteCode     邀请码
     * @return 结果
     */
    public CompletableFuture<Boolean> registerUser(AccountContext accountContext, String inviteCode) {
        try {
            JSONObject account = new JSONObject();
            String email = accountContext.getAccountBaseInfo().getEmail();
            String name = accountContext.getName();
            String password = accountContext.getAccountBaseInfo().getPassword();

            account.put("username", email);
            account.put("name", name);
            account.put("password", password);
            account.put("inviteCode", inviteCode);

            Map<String, String> headers = getHeaders(accountContext);


            return openLoopDepinBot
                    .syncRequest(
                            accountContext.getProxy(),
                            "https://api.openloop.so/users/register",
                            "post",
                            headers,
                            null,
                            account,
                            () -> String.format("开始注册[%s]-email[%s]", name, email)
                    )
                    .thenApplyAsync(responseStr -> {
                        JSONObject body = JSONObject.parseObject(responseStr);

                        if (body.getInteger("code") == 2000) {
                            log.info("注册邮箱[{}]成功！请前往验证", email);
                            return true;
                        } else {
                            log.error("注册[{}]失败, {}", email, responseStr);
                            return false;
                        }
                    });
        } catch (Exception e) {
            log.error("注册[{}]发生未知错误", accountContext.getAccountBaseInfo().getEmail(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 登录用户,返回token
     *
     * @param accountContext accountContext
     * @return 结果
     */
    public CompletableFuture<String> loginUser(AccountContext accountContext) {

        JSONObject account = new JSONObject();
        String email = accountContext.getAccountBaseInfo().getEmail();
        account.put("username", email);
        account.put("password", accountContext.getAccountBaseInfo().getPassword());

        Map<String, String> headers = getHeaders(accountContext);

        return openLoopDepinBot
                .syncRequest(
                        accountContext.getProxy(),
                        "https://api.openloop.so/users/login",
                        "post",
                        headers,
                        null,
                        account,
                        () -> String.format("账户[%s]开始获取token", email)
                )
                .thenApplyAsync(responseStr -> {
                    JSONObject body = JSONObject.parseObject(responseStr);

                    if (body.getInteger("code") == 2000) {
                        log.info("账户[{}]-proxy[{}] token获取成功", email, accountContext.getProxy().getAddressStr());
                        return body.getJSONObject("data").getString("accessToken");
                    } else {
                        throw new LoginException("登录失败，服务器响应结果：" + responseStr);
                    }
                });
    }

    /**
     * 分享带宽
     *
     * @param accountContext accountContext
     * @return JSONObject
     */
    public CompletableFuture<Boolean> shareBandwidth(AccountContext accountContext) {
        String token = accountContext.getParam("token");

        JSONObject body = new JSONObject();
        body.put("quality", getRandomQuality());

        Map<String, String> headers = getHeaders(accountContext);
        headers.put("Authorization", "Bearer " + token);
        headers.put("origin", "chrome-extension://effapmdildnpkiaeghlkicpfflpiambm");
        NetworkProxy proxy = accountContext.getProxy();
        String address = proxy.getHost() + ":" + proxy.getPort();

        return openLoopDepinBot
                .syncRequest(
                        proxy,
                        "https://api.openloop.so/bandwidth/share",
                        "post",
                        headers,
                        null,
                        body,
                        () -> String.format("账户[%s]-[%s]开始ping", accountContext.getName(), address)
                )
                .thenApplyAsync((responseStr) -> {
                    if (responseStr != null) {
                        log.info("账户[{}]-[{}]ping 成功, response: {}",
                                accountContext.getName(), address, responseStr);
                    } else {
                        log.error("账户[{}]-[{}]ping 失败, response: {}",
                                accountContext.getName(), address, responseStr);
                    }
                    return true;
                });
    }

    @NotNull
    private static Map<String, String> getHeaders(AccountContext accountContext) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("referer", "https://openloop.so/");
        headers.put("origin", "https://openloop.so");
        headers.put("user-agent", accountContext.getBrowserEnv().getHeaders().get("user-agent"));
        return headers;
    }

    /**
     * 获取随机的网络质量
     *
     * @return 60-99
     */
    private int getRandomQuality() {
        return random.nextInt(40) + 60;
    }
}
