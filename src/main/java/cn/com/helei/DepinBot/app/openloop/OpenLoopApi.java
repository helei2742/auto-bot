//package cn.com.helei.DepinBot.openloop;
//
//import cn.com.helei.DepinBot.core.dto.account.AccountContext;
//import cn.com.helei.DepinBot.core.exception.LoginException;
//import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
//import com.alibaba.fastjson.JSONObject;
//import lombok.extern.slf4j.Slf4j;
//import org.jetbrains.annotations.NotNull;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Random;
//import java.util.concurrent.CompletableFuture;
//
//@Slf4j
//public class OpenLoopApi {
//
//    private static final Random random = new Random();
//
//    private final OpenLoopDepinBot openLoopDepinBot;
//
//    public OpenLoopApi(OpenLoopDepinBot openLoopDepinBot) {
//        this.openLoopDepinBot = openLoopDepinBot;
//    }
//
//
//    /**
//     * 注册用户
//     *
//     * @param accountContext accountContext
//     * @param inviteCode     邀请码
//     * @return 结果
//     */
//    public CompletableFuture<Boolean> registerUser(AccountContext accountContext, String inviteCode) {
//        try {
//            JSONObject account = new JSONObject();
//            String email = accountContext.getClientAccount().getEmail();
//            String name = accountContext.getName();
//            String password = accountContext.getClientAccount().getPassword();
//
//            account.put("username", email);
//            account.put("name", name);
//            account.put("password", password);
//            account.put("inviteCode", inviteCode);
//
//            Map<String, String> headers = getHeaders(accountContext);
//
//
//            return openLoopDepinBot
//                    .syncRequest(
//                            accountContext.getProxy(),
//                            "https://api.openloop.so/users/register",
//                            "post",
//                            headers,
//                            null,
//                            account,
//                            () -> String.format("开始注册[%s]-email[%s]", name, email)
//                    )
//                    .thenApplyAsync(response -> {
//                        JSONObject jsonObject;
//                        if (response.isSuccessful()
//                                && response.body() != null
//                                && (jsonObject = JSONObject.parseObject(response.body().toString())) != null
//                                && jsonObject.getInteger("code") == 2000
//                        ) {
//                            log.info("注册邮箱[{}]成功！请前往验证", email);
//                            return true;
//                        } else {
//                            log.error("注册[{}]失败, {}", email, response.body());
//                            return false;
//                        }
//                    });
//        } catch (Exception e) {
//            log.error("注册[{}]发生未知错误", accountContext.getClientAccount().getEmail(), e);
//            return CompletableFuture.completedFuture(false);
//        }
//    }
//
//    /**
//     * 登录用户,返回token
//     *
//     * @param accountContext accountContext
//     * @return 结果
//     */
//    public CompletableFuture<String> loginUser(AccountContext accountContext) {
//
//        JSONObject account = new JSONObject();
//        String email = accountContext.getClientAccount().getEmail();
//        account.put("username", email);
//        account.put("password", accountContext.getClientAccount().getPassword());
//
//        Map<String, String> headers = getHeaders(accountContext);
//
//        return openLoopDepinBot
//                .syncRequest(
//                        accountContext.getProxy(),
//                        "https://api.openloop.so/users/login",
//                        "post",
//                        headers,
//                        null,
//                        account,
//                        () -> String.format("账户[%s]开始获取token", email)
//                )
//                .thenApplyAsync((response) -> {
//                    JSONObject jsonObject;
//
//                    if (response.isSuccessful()
//                            && response.body() != null
//                            && (jsonObject = JSONObject.parseObject(response.body().toString())) != null
//                            && jsonObject.getInteger("code") == 2000
//                    ) {
//                        return jsonObject.getJSONObject("data").getString("accessToken");
//                    } else {
//                        throw new LoginException("登录失败，服务器响应结果：" + response.body());
//                    }
//                });
//    }
//
//    /**
//     * 分享带宽
//     *
//     * @param accountContext accountContext
//     * @return JSONObject
//     */
//    public CompletableFuture<JSONObject> shareBandwidth(AccountContext accountContext) {
//        JSONObject body = new JSONObject();
//        body.put("quality", getRandomQuality());
//
//        Map<String, String> headers = getHeaders(accountContext);
//        headers.put("Authorization", "Bearer " + accountContext.getParam("token"));
//        headers.put("origin", "chrome-extension://effapmdildnpkiaeghlkicpfflpiambm");
//        NetworkProxy proxy = accountContext.getProxy();
//        String address = proxy.getHost() + ":" + proxy.getPort();
//
//        return openLoopDepinBot
//                .syncRequest(
//                        proxy,
//                        "https://api.openloop.so/bandwidth/share",
//                        "post",
//                        headers,
//                        null,
//                        body,
//                        () -> String.format("账户[%s]-[%s]开始分享带宽", accountContext.getName(), address)
//                )
//                .thenApplyAsync((response) -> {
//                    if (response.isSuccessful()) {
//                        String bodyStr = response.body() != null ? response.body().toString() : "";
//                        log.info("账户[{}]-[{}]分享带宽成功,response: {}", accountContext.getName(), address, bodyStr);
//
//                        return new JSONObject();
//                    } else {
//                        throw new RuntimeException(String.format("账户[%s]-[%s]分享带宽失败", accountContext.getName(), address));
//                    }
//                });
//    }
//
//    @NotNull
//    private static Map<String, String> getHeaders(AccountContext accountContext) {
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Content-Type", "application/json; charset=utf-8");
//        headers.put("referer", "https://openloop.so/");
//        headers.put("origin", "https://openloop.so");
//        headers.put("user-agent", accountContext.getBrowserEnv().getHeaders().get("user-agent"));
//        return headers;
//    }
//
//    /**
//     * 获取随机的网络质量
//     *
//     * @return 60-99
//     */
//    private int getRandomQuality() {
//        return random.nextInt(40) + 60;
//    }
//}
