package cn.com.helei.application.ecosapiens;

import cn.com.helei.bot.core.bot.RestTaskAutoBot;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.config.BaseAutoBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.exception.RegisterException;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.com.helei.bot.core.supporter.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@Slf4j
public class EcosapiensRegisterBot extends RestTaskAutoBot {

    private final Semaphore semaphore;

    public EcosapiensRegisterBot(BaseAutoBotConfig baseAutoBotConfig) {
        super(baseAutoBotConfig);

        semaphore = new Semaphore(10);
    }


    @Override
    protected void typedAccountsLoadedHandler(Map<String, List<AccountContext>> typedAccountMap) {
        Map<String, List<String>> map = (Map<String, List<String>>) getBaseAutoBotConfig().getConfigMap().get("ethAddress");

        typedAccountMap.forEach((type, accountContextList) -> {
            List<String> address = map.get(type);

            for (int i = 0; i < Math.min(accountContextList.size(), address.size()); i++) {
                accountContextList.get(i).setParam("ethAddress", address.get(i));
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String refLink = getBaseAutoBotConfig().getConfig("referral_link");

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();

        headers.put("content-type", "application/json");
        JSONObject heartbeatBody = new JSONObject();

        heartbeatBody.put("location", refLink);
        heartbeatBody.put("waitlist_id", "24495");
        heartbeatBody.put("widget_type", "WIDGET_1");
        heartbeatBody.put("referrer", "");
        // 设置代理和身份验证
        NetworkProxy proxy = accountContext.getProxy();

        HttpClient client = HttpClient.newHttpClient();

        return CompletableFuture
                .supplyAsync(() -> {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.getwaitlist.com/api/v1/widget_heartbeats"))
                            .POST(HttpRequest.BodyPublishers.ofString(heartbeatBody.toJSONString()))
                            .setHeader("content-type", "application/json")
                            .setHeader("origin", "https://ecosapiens.xyz")
                            .setHeader("priority", "u=1, i")
                            .setHeader("referer", "https://ecosapiens.xyz/")
                            .setHeader("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                            .setHeader("sec-ch-ua-mobile", "?0")
                            .setHeader("sec-ch-ua-platform", "\"macOS\"")
                            .setHeader("sec-fetch-dest", "empty")
                            .setHeader("sec-fetch-mode", "cors")
                            .setHeader("sec-fetch-site", "cross-site")
                            .setHeader("user-agent", headers.get("user-agent"))
                            .build();

                    HttpResponse<String> response = null;
                    try {
                        response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == 200) {
                            JSONObject jb = JSONObject.parseObject(response.body());
                            return jb.getString("uuid");
                        }
                       throw new RegisterException("注册异常 " + response.statusCode() + " " + response.body());
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException("获取uuid发生异常", e);
                    }
                }, getExecutorService())
                .thenApplyAsync(uuid -> {
                    JSONObject body = new JSONObject();
                    body.put("waitlist_id", 24495);
                    body.put("referral_link", refLink);
                    body.put("heartbeat_uuid", uuid);
                    body.put("widget_type", "WIDGET_1");
                    body.put("email", accountContext.getAccountBaseInfo().getEmail());
                    JSONArray ja = new JSONArray();
                    JSONObject jb = new JSONObject();
                    jb.put("question_value", "What is your EVM wallet address?");
                    jb.put("answer_value", accountContext.getParam("ethAddress"));
                    ja.add(jb);
                    body.put("answers", ja);

                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.getwaitlist.com/api/v1/waiter"))
                                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                                .setHeader("content-type", "application/json")
                                .setHeader("origin", "https://ecosapiens.xyz")
                                .setHeader("priority", "u=1, i")
                                .setHeader("referer", "https://ecosapiens.xyz/")
                                .setHeader("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                                .setHeader("sec-ch-ua-mobile", "?0")
                                .setHeader("sec-ch-ua-platform", "\"macOS\"")
                                .setHeader("sec-fetch-dest", "empty")
                                .setHeader("sec-fetch-mode", "cors")
                                .setHeader("sec-fetch-site", "cross-site")
                                .setHeader("user-agent", headers.get("user-agent"))
                                .build();

                        HttpResponse<String> response = null;
                        response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        String respStr = response.body();

                        if (StrUtil.isNotBlank(respStr)) {
                            accountContext.setParam("ecosapiens_register_info", respStr);
                            log.info("{} 注册ecosapiens成功, {}", accountContext.getSimpleInfo(), respStr);
                            return true;
                        }
                        throw new RegisterException("注册失败，" + respStr);
                    } catch (InterruptedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }, getExecutorService()).whenComplete((s, throwable) -> {
                    semaphore.release();
                });
    }

    @Override
    public CompletableFuture<Boolean> verifierAccountEmail(AccountContext accountContext, Message message) {
        return null;
    }

    @Override
    public CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return null;
    }

    @Override
    public boolean doAccountClaim(AccountContext accountContext) {
        return false;
    }

    @Override
    public CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
        return null;
    }


    public static void main(String[] args) throws DepinBotStartException {
        BaseAutoBotConfig config = YamlConfigLoadUtil.load(
                SystemConfig.CONFIG_DIR_APP_PATH,
                "ecosapiens.yaml",
                List.of("bot", "app", "ecosapiens"),
                BaseAutoBotConfig.class
        );

        EcosapiensRegisterBot ecosapiensRegisterBot = new EcosapiensRegisterBot(config);

        MenuCMDLineAutoBot<BaseAutoBotConfig> menuCMDLineAutoBot
                = new MenuCMDLineAutoBot<>(ecosapiensRegisterBot, List.of(DefaultMenuType.REGISTER));

        menuCMDLineAutoBot.start();
    }
}
