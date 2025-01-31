package cn.com.helei.application.teneo;

import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.LoginException;
import cn.com.helei.bot.core.exception.RegisterException;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Slf4j
public class TeneoApi {

    private static final String EMAIL_VERIFIED_KEY = "email_verified";

    private final TeneoWSDepinBot bot;

    public TeneoApi(TeneoWSDepinBot bot) {
        this.bot = bot;
    }

    public CompletableFuture<String> login(AccountContext accountContext) {
        String url = "https://auth.teneo.pro/api/login";

        NetworkProxy proxy = accountContext.getProxy();

        JSONObject body = new JSONObject();
        body.put("email", accountContext.getAccountBaseInfo().getEmail());
        String password = accountContext.getAccountBaseInfo().getPassword();
        if (!password.matches(".*[0-9].*")) {
            password = password + "1";
        }
        body.put("password", password);

        String printStr = String.format("账户[%s]-proxy[%s:{%d]",
                accountContext.getAccountBaseInfo().getEmail(), proxy.getHost(), proxy.getPort());

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("origin", "https://dashboard.teneo.pro");
        headers.put("referer", "https://dashboard.teneo.pro/");
        headers.put("X-API-KEY", bot.getBotConfig().getApiKey());

        return bot.syncRequest(
                proxy,
                url,
                "post",
                headers,
                null,
                body,
                () -> printStr
        ).thenApplyAsync(responseStr -> {
            JSONObject response = JSONObject.parseObject(responseStr);
            if (response != null && responseStr.contains("access_token")) {
                String string = response.getString("access_token");
                log.info("{} token 获取成功,{}", printStr, string);
                return string;
            } else {
                throw new LoginException(printStr + " 登录获取token失败, response: " + responseStr);
            }
        });
    }


    /**
     * 注册账户
     *
     * @param accountContext accountContext
     * @param inviteCode     inviteCode
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        if (accountContext.getAccountBaseInfo().getSignUp()) {
            return CompletableFuture.completedFuture(true);
        }

        String url = "https://node-b.teneo.pro/auth/v1/signup";

        JSONObject body = new JSONObject();
        body.put("email", accountContext.getAccountBaseInfo().getEmail());
        body.put("password", accountContext.getAccountBaseInfo().getPassword());

        JSONObject data = new JSONObject();
        data.put("invited_by", inviteCode);
        body.put("data", data);

        body.put("gotrue_meta_security", new JSONObject());
        body.put("code_challenge", null);
        body.put("code_challenge_method", null);


        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("origin", "https://dashboard.teneo.pro");
        headers.put("referer", "https://dashboard.teneo.pro/");
        headers.put("X-API-KEY", bot.getBotConfig().getApiKey());

        String simpleInfo = accountContext.getSimpleInfo();

        return bot.syncRequest(
                accountContext.getProxy(),
                url,
                "post",
                headers,
                null,
                body,
                () -> simpleInfo + " "
        ).thenApplyAsync(responseStr -> {
            try {
                JSONObject response = JSONObject.parseObject(responseStr);
                JSONObject userMetadata = response.getJSONObject("user_metadata");
                log.info("{} 注册成功 [{}]", simpleInfo, userMetadata);

                accountContext.setParam(EMAIL_VERIFIED_KEY, String.valueOf(userMetadata.getBoolean(EMAIL_VERIFIED_KEY)));

                return true;
            } catch (Exception e) {
                throw new RegisterException(String.format("%s 注册发生异常", simpleInfo), e);
            }
        });
    }

    public String verifierEmail() {

//        MailReaderFactory.getMailReader(MailProtocolType.imap, );


//        bot.getAccounts().stream()
//                .filter(accountContext -> {
//                    String str = accountContext.getParam(EMAIL_VERIFIED_KEY);
//                    // 注册的、验证状态为false的才需要验证邮件
//                    return BooleanUtil.isTrue(accountContext.getClientAccount().getSignUp())
//                            && str != null && BooleanUtil.isFalse(Boolean.valueOf(str));
//                })
//                .map(accountContext -> {
//
//                });

        return null;
    }
}
