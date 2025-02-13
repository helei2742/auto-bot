package cn.com.helei.bot.app.teneo;

import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.anno.BotWSMethodConfig;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;
import cn.com.helei.bot.core.util.exception.LoginException;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@BotApplication(name = "Teneo_Bot")
public class TeneoBot extends AnnoDriveAutoBot<TeneoBot> {

    private static final String LOGIN_API = "https://auth.teneo.pro/api/login";

    private static final String TOKEN_KEY = "token";

    private static final Logger log = LoggerFactory.getLogger(TeneoBot.class);

    public TeneoBot(AutoBotConfig autoBotConfig, BotApi botApi) {
        super(autoBotConfig, botApi);
    }

    @Override
    protected TeneoBot getInstance() {
        return this;
    }


    @BotMethod(jobType = BotJobType.LOGIN)
    protected Result login(AccountContext accountContext) {

        ProxyInfo proxy = accountContext.getProxy();

        JSONObject body = new JSONObject();
        body.put("email", accountContext.getAccountBaseInfo().getEmail());
        body.put("password", accountContext.getAccountBaseInfo().getPassword());

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();

        CompletableFuture<Result> future = syncRequest(
                proxy,
                LOGIN_API,
                HttpMethod.POST,
                headers,
                null,
                body,
                () -> accountContext.getSimpleInfo() + " 开始登录"
        ).thenApplyAsync(responseStr -> {
            JSONObject response = JSONObject.parseObject(responseStr);
            if (response != null && responseStr.contains("access_token")) {
                String token = response.getString("access_token");

                accountContext.setParam(TOKEN_KEY, token);

                return Result.ok(token);
            } else {
                throw new LoginException(accountContext.getSimpleInfo() + " 登录获取token失败, response: " + responseStr);
            }
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("{} 出现异常登录失败, {}", accountContext, e.getMessage());
            return Result.fail("%s 现异常登录失败".formatted(accountContext.getSimpleInfo()));
        }
    }

    @BotMethod(
            jobType = BotJobType.WEB_SOCKET_CONNECT,
            bowWsConfig = @BotWSMethodConfig(
                    heartBeatIntervalSecond = 10
            )
    )
    public BaseBotWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {
        String connectUrl = "wss://secure.ws.teneo.pro/websocket?accessToken=" + accountContext.getParam("token") + "&version=v0.2";

        TeneoWSClient teneoWSClient = new TeneoWSClient(accountContext, connectUrl);

        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        Map<String, String> originHeaders = accountContext.getBrowserEnv().getHeaders();
        originHeaders.forEach(headers::add);

        headers.add("Host", "secure.ws.teneo.pro");
        headers.add("Origin", "chrome-extension://emcclcoaglgcpoognfiggmhnhgabppkm");
        headers.add("Upgrade", "websocket");

        teneoWSClient.setHeaders(headers);

        return teneoWSClient;
    }
}
