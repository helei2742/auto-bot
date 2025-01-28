package cn.com.helei.application.oasis;

import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.LoginException;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OasisApi {


    private final OasisDepinBot oasisDepinBot;

    public OasisApi(OasisDepinBot oasisDepinBot) {
        this.oasisDepinBot = oasisDepinBot;
    }


    /**
     * 注册用户
     *
     * @param accountContext accountContext
     * @param inviteCode     邀请码
     * @return 结果
     */
    public CompletableFuture<Boolean> registerUser(AccountContext accountContext, String inviteCode) {

        JSONObject account = new JSONObject();
        String email = accountContext.getClientAccount().getEmail();
        account.put("email", email);
        account.put("password", accountContext.getClientAccount().getPassword());
        account.put("referralCode", inviteCode);

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("Content-Type", "application/json; charset=utf-8");

        return oasisDepinBot.syncRequest(
                        accountContext.getProxy(),
                        "https://api.oasis.ai/internal/auth/signup",
                        "post",
                        headers,
                        null,
                        account
                )
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
     * @param accountContext    accountContext
     * @return 结果
     */
    public CompletableFuture<String> loginUser(AccountContext accountContext) {

        JSONObject account = new JSONObject();
        account.put("email", accountContext.getClientAccount().getEmail());
        account.put("password", accountContext.getClientAccount().getPassword());
        account.put("rememberSession", true);

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("Content-Type", "application/json; charset=utf-8");

        return oasisDepinBot.syncRequest(
                        accountContext.getProxy(),
                        "https://api.oasis.ai/internal/auth/login",
                        "post",
                        headers,
                        null,
                        account
                )
                .thenApplyAsync((res) -> {
                    JSONObject result = JSONObject.parseObject(res);
                    String token = result.getString("token");

                    if (StrUtil.isNotBlank(token)) {
                        log.info("邮箱[{}]登录成功，token[{}]", accountContext.getClientAccount().getEmail(), token);
                        accountContext.setParam("token", token);
                        return token;
                    }
                    throw new LoginException("登录失败, " + res);
                });
    }

    /**
     * 登录用户,返回token
     *
     * @param accountContext accountContext
     * @return 结果
     */
    public CompletableFuture<Boolean> resendCode(AccountContext accountContext) {

        JSONObject account = new JSONObject();
        account.put("email", accountContext.getClientAccount().getEmail());

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("Content-Type", "application/json; charset=utf-8");

        return oasisDepinBot
                .syncRequest(
                        accountContext.getProxy(),
                        "https://api.oasis.ai/internal/auth/resend-code",
                        "post",
                        headers,
                        null,
                        account
                )
                .thenApplyAsync((res) -> {
                    if (StrUtil.isBlank(res)) return true;
                    else {
                        throw new RuntimeException("重发验证码错误，" + res);
                    }
                });
    }
}
