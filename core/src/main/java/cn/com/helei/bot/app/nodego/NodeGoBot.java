package cn.com.helei.bot.app.nodego;

import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.util.captcha.CloudFlareResolver;
import cn.com.helei.bot.core.util.exception.RegisterException;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@BotApplication(name = "NodeGO", describe = "NodeGo自动机器人")
public class NodeGoBot extends AnnoDriveAutoBot<NodeGoBot> {

    private static final String PASSWORD_KEY = "password";

    private static final String ACCESS_TOKEN_KEY = "accessToken";

    private static final String REGISTER_WEBSITE_KEY = "0x4AAAAAAA4zgfgCoYChIZf4";

    private static final String REGISTER_WEBSITE_URL = "https://app.nodego.ai/register";

    private static final String REGISTER_API = "https://nodego.ai/api/auth/register";

    private static final String LOGIN_API = "https://nodego.ai/api/auth/login";

    private static final String CHECK_IN_API = "https://nodego.ai/api/user/checkin";

    private static final String KEEP_ALIVE_API = "https://nodego.ai/api/user/nodes/ping";

    private static final int KEEP_ALIVE_INTERVAL = 60;


    private String twoCaptchaApiKey = "";

    public NodeGoBot(AutoBotConfig autoBotConfig, BotApi botApi) {
        super(autoBotConfig, botApi);
        this.twoCaptchaApiKey = autoBotConfig.getConfig("2_CAPTCHA_API_KEY");
    }

    @Override
    protected NodeGoBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.REGISTER)
    public Result autoRegister(AccountContext accountContext, String inviteCode) {
        String email = accountContext.getAccountBaseInfo().getEmail();
        String username = email.split("@")[0];
        String password = accountContext.getParam(PASSWORD_KEY);

        log.info("{} 开始打码获取token", username);
        CompletableFuture<Result> future = CloudFlareResolver.cloudFlareResolve(
                        accountContext.getProxy(),
                        REGISTER_WEBSITE_URL,
                        REGISTER_WEBSITE_KEY,
                        twoCaptchaApiKey
                )
                .thenApplyAsync(tokenAndUA -> {
                    log.info("{} 开始打码成功， result:[{}]", username, tokenAndUA);

                    String token = tokenAndUA.getString("token");
                    String userAgent = tokenAndUA.getString("userAgent");

                    JSONObject body = new JSONObject();

                    body.put("username", username);
                    body.put("email", email);
                    body.put("password", password);
                    body.put("refBy", inviteCode);
                    body.put("captcha", token);

                    // 请求注册
                    Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
                    headers.put("User-Agent", userAgent);
                    headers.put("Origin", "https://app.nodego.ai");
                    headers.put("Accept", "application/json");
                    headers.put("referer", "https://app.nodego.ai/");

                    try {
                        return syncRequest(
                                accountContext.getProxy(),
                                REGISTER_API,
                                "post",
                                headers,
                                null,
                                body,
                                () -> accountContext.getSimpleInfo() + " 开始注册"
                        ).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RegisterException(accountContext.getSimpleInfo() + " 注册失败, " + e.getMessage());
                    }
                })
                .thenApplyAsync(registerResultStr -> {
                    JSONObject registerResult = JSONObject.parseObject(registerResultStr);

                    Integer statusCode = registerResult.getInteger("statusCode");
                    if (statusCode == 201) {
                        log.info("{} 注册成功, {}", accountContext.getSimpleInfo(), registerResultStr);

                        accountContext.setParam(ACCESS_TOKEN_KEY, registerResult
                                .getJSONObject("metadata").getString(ACCESS_TOKEN_KEY));

                        AccountContext.signUpSuccess(accountContext);

                        return Result.ok();
                    } else {
                        throw new RegisterException("注册失败, " + registerResultStr);
                    }
                })
                .exceptionallyAsync(throwable -> {
                    log.error("{} 注册发生异常", accountContext.getSimpleInfo(), throwable);

                    String exception = throwable.getMessage();
                    if (exception.contains("{\"message\":\"Email already exists\",\"error\":\"Bad Request\",\"statusCode\":400}")) {
                        log.warn("{} 已注册过", accountContext.getSimpleInfo());
                        AccountContext.signUpSuccess(accountContext);
                        return Result.ok();
                    }
                    return Result.fail("注册发生异常, " + exception);
                });

        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return Result.fail("未知错误, " + e.getMessage());
        }
    }


    @BotMethod(jobType = BotJobType.LOGIN)
    public Result login(AccountContext accountContext) {
        if (accountContext.getParams().containsKey(ACCESS_TOKEN_KEY)) {
            log.warn("{} 已存在token", accountContext.getSimpleInfo());
            return Result.ok();
        }

        String email = accountContext.getAccountBaseInfo().getEmail();
        String password = accountContext.getParam(PASSWORD_KEY);

        log.info("{} 开始打码获取token", email);
        CompletableFuture<Result> future = CloudFlareResolver.cloudFlareResolve(
                        accountContext.getProxy(),
                        REGISTER_WEBSITE_URL,
                        REGISTER_WEBSITE_KEY,
                        twoCaptchaApiKey
                )
                .thenApplyAsync(tokenAndUA -> {
                    log.info("{} 开始打码成功， result:[{}]", email, tokenAndUA);

                    String token = tokenAndUA.getString("token");
                    String userAgent = tokenAndUA.getString("userAgent");

                    // 请求注册
                    Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
                    headers.put("User-Agent", userAgent);
                    headers.put("Origin", "https://app.nodego.ai");
                    headers.put("Accept", "application/json");
                    headers.put("referer", "https://app.nodego.ai/");


                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    body.put("password", password);
                    body.put("captcha", token);
                    try {
                        return syncRequest(
                                accountContext.getProxy(),
                                LOGIN_API,
                                "post",
                                headers,
                                null,
                                body,
                                () -> accountContext.getSimpleInfo() + " 开始登录"
                        ).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RegisterException(accountContext.getSimpleInfo() + " 登录失败, " + e.getMessage());
                    }
                })
                .thenApplyAsync(registerResultStr -> {
                    JSONObject registerResult = JSONObject.parseObject(registerResultStr);

                    Integer statusCode = registerResult.getInteger("statusCode");
                    if (statusCode == 201) {
                        log.info("{} 登录成功, {}", accountContext.getSimpleInfo(), registerResultStr);

                        accountContext.setParam(ACCESS_TOKEN_KEY, registerResult
                                .getJSONObject("metadata").getString(ACCESS_TOKEN_KEY));

                        return Result.ok();
                    } else {
                        throw new RegisterException("登录失败, " + registerResultStr);
                    }
                })
                .exceptionallyAsync(throwable -> {
                    log.error("{} 登录发生异常, {}", accountContext.getSimpleInfo(), throwable.getMessage());
                    return Result.fail("登录发生异常," + throwable.getMessage());
                });;

        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return Result.fail("未知错误, " + e.getMessage());
        }
    }
}

