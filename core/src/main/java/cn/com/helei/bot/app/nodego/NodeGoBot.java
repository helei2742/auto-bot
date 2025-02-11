package cn.com.helei.bot.app.nodego;

import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.util.captcha.TwoCaptchaSolverFactory;
import cn.com.helei.bot.core.util.exception.RegisterException;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.twocaptcha.TwoCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@BotApplication(name = "NodeGO", describe = "NodeGo自动机器人")
public class NodeGoBot extends AnnoDriveAutoBot<NodeGoBot> {

    private static final String PASSWORD_KEY = "password";

    private static final String ACCESS_TOKEN_KEY = "access_token";

    private static final String REGISTER_WEBSITE_KEY = "0x4AAAAAAA4zgfgCoYChIZf4";

    private static final String REGISTER_WEBSITE = "https://app.nodego.ai/register";

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

        CompletableFuture<Result> future = cloudFlareResolve(accountContext)
                .thenApplyAsync(tokenAndUA -> {
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
                                REGISTER_WEBSITE,
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

                    if (registerResult.getInteger("statusCode") == 201) {
                        log.info("{} 注册成功, {}", accountContext.getSimpleInfo(), registerResultStr);

                        accountContext.setParam(ACCESS_TOKEN_KEY, registerResult
                                .getJSONObject("metadata").getString("access_token"));

                        return Result.ok();
                    } else {
                        throw new RegisterException("注册失败, " + registerResultStr);
                    }
                })
                .exceptionallyAsync(throwable -> {
                    log.error("{} 注册发生异常", accountContext.getSimpleInfo(), throwable);
                    return Result.fail("注册发生异常, " + throwable.getMessage());
                });

        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return Result.fail("未知错误, " + e.getMessage());
        }
    }


    private CompletableFuture<JSONObject> cloudFlareResolve(AccountContext accountContext) {
        JSONObject body = new JSONObject();
        ProxyInfo proxy = accountContext.getProxy();

        body.put("clientKey", twoCaptchaApiKey);
        JSONObject task = getTaskInfo(proxy);

        body.put("task", task);

        return syncRequest(
                proxy,
                "https://api.2captcha.com/createTask",
                "post",
                accountContext.getBrowserEnv().getHeaders(),
                null,
                body,
                () -> accountContext.getSimpleInfo() + " 开始打码"
        ).thenApplyAsync(resultStr -> {
            JSONObject result = JSONObject.parseObject(resultStr);

            String taskId = result.getString("taskId");

            if (StrUtil.isNotBlank(taskId)) {
                log.info("{} 打码task创建成功, taskId[{}]", accountContext.getSimpleInfo(), taskId);

                try {
                    String codeStr = solver.getResult(taskId);
                    JSONObject code = JSONObject.parseObject(codeStr);
                    if ("ready".equals(code.getString("status"))) {
                        log.info("{} 打码成功，{}", accountContext.getSimpleInfo(), codeStr);

                        return code.getJSONObject("solution");
                    }

                } catch (Exception e) {
                    throw new RuntimeException("打码失败, task结果获取失败", e);
                }
                return result.getJSONObject("solution");
            }
            throw new RuntimeException("打码失败, task创建失败" + resultStr);
        });
    }

    private static CompletableFuture<String> get

    @NotNull
    private static JSONObject getTaskInfo(ProxyInfo proxy) {
        JSONObject task = new JSONObject();
        task.put("type", "TurnstileTask");
        task.put("websiteURL", REGISTER_WEBSITE);
        task.put("websiteKey", REGISTER_WEBSITE_KEY);

        if (proxy != null) {
            task.put("proxyType", "http");
            task.put("proxyAddress", proxy.getHost());
            task.put("proxyPort", String.valueOf(proxy.getPort()));
            task.put("proxyLogin", proxy.getUsername());
            task.put("proxyPassword", proxy.getPassword());
        }
        return task;
    }

}

