package cn.com.helei.application.teneo;

import cn.com.helei.bot.core.bot.base.AccountManageAutoBot;
import cn.com.helei.bot.core.constants.MapConfigKey;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.LoginException;
import cn.com.helei.bot.core.exception.RegisterException;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;



@Slf4j
public class TeneoApi {

    private final TeneoWSAutoBot bot;

    public TeneoApi(TeneoWSAutoBot bot) {
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
//        if (accountContext.getAccountBaseInfo().getId() != 0) return CompletableFuture.completedFuture(false);

        if (BooleanUtil.isTrue(accountContext.getAccountBaseInfo().getSignUp())) {
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
        headers.put("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imlra25uZ3JneHV4Z2pocGxicGV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MjU0MzgxNTAsImV4cCI6MjA0MTAxNDE1MH0.DRAvf8nH1ojnJBc3rD_Nw6t1AV8X_g6gmY_HByG2Mag");
        headers.put("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imlra25uZ3JneHV4Z2pocGxicGV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MjU0MzgxNTAsImV4cCI6MjA0MTAxNDE1MH0.DRAvf8nH1ojnJBc3rD_Nw6t1AV8X_g6gmY_HByG2Mag");
        headers.put("X-API-KEY", bot.getBotConfig().getApiKey());

        String simpleInfo = accountContext.getSimpleInfo();

        return bot.syncRequest(
                accountContext.getProxy(),
                url,
                "post",
                headers,
                null,
                body,
                () -> simpleInfo + " 开始注册"
        ).thenApplyAsync(responseStr -> {
            try {
                JSONObject response = JSONObject.parseObject(responseStr);
                JSONObject userMetadata = response.getJSONObject("user_metadata");
                log.info("{} 注册成功 [{}]", simpleInfo, userMetadata);

                accountContext.setParam(MapConfigKey.EMAIL_VERIFIED_KEY,
                        String.valueOf(userMetadata.getBoolean(MapConfigKey.EMAIL_VERIFIED_KEY)));

                return true;
            } catch (Exception e) {
                throw new RegisterException(String.format("%s 注册发生异常", simpleInfo), e);
            }
        });
    }


    public CompletableFuture<Boolean> verifierEmail(AccountContext accountContext, Message message) {
        String link = resolveLinkFromMessage(message);

        if (StrUtil.isBlank(link)) return CompletableFuture.completedFuture(false);

        return bot.syncRequest(
                        accountContext.getProxy(),
                        link,
                        "get",
                        accountContext.getBrowserEnv().getHeaders(),
                        null,
                        null,
                        () -> accountContext.getSimpleInfo() + " 进入验证链接"
                ).thenApplyAsync(res -> {
                    log.info("{} 邮件验证完成，", accountContext.getSimpleInfo());
                    return true;
                }, bot.getExecutorService())
                .exceptionally(throwable -> {
                    log.error("{} 邮件验证失败", accountContext.getSimpleInfo(), throwable);
                    return false;
                });
    }


    private String resolveLinkFromMessage(Message message) {
        String xpath = "body > div > table > tbody > tr > td > table.es-content > tbody > tr > td > table > tbody > tr > td > table > tbody > tr > td > table > tbody > tr:nth-child(2) > td > span > a";

        try {
            boolean b = Arrays.stream(message.getFrom())
                    .anyMatch(address -> "TENEO Community Node <noreply@norply.teneo.pro>".equals(address.toString()));
            if (!b) return null;

            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();

            String htmlStr = mimeMultipart.getBodyPart(1).getContent().toString();

            Document document = Jsoup.parse(htmlStr);
            // 使用 CSS 选择器提取 a 标签内容
            Elements linkElement = document.select(xpath);
            return linkElement.attr("href");
        } catch (Exception e) {
            throw new RuntimeException("从邮件提取链接出错", e);
        }
    }
}
