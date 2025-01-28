package cn.com.helei.application.oasis;

import cn.com.helei.bot.core.BaseDepinWSClient;
import cn.com.helei.bot.core.SimpleDepinWSClient;
import cn.com.helei.bot.core.bot.WSMenuCMDLineDepinBot;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.RewordInfo;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.netty.constants.WebsocketClientStatus;
import cn.com.helei.bot.core.util.SystemInfo;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class OasisDepinBot extends WSMenuCMDLineDepinBot<OasisBotConfig, JSONObject, JSONObject> {

    private static final String TOKEN_KEY = "token";

    private final OasisApi oasisApi;

    public OasisDepinBot(String oasisBotConfigPath) {
        super(OasisBotConfig.loadYamlConfig(oasisBotConfigPath));
        this.oasisApi = new OasisApi(this);
    }

    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {
        defaultMenuTypes.add(DefaultMenuType.START_ACCOUNT_CLAIM);
        defaultMenuTypes.add(DefaultMenuType.LOGIN);

        CommandMenuNode resendCode = new CommandMenuNode(true, "重发验证邮件",
                "开始重发验证邮件", this::resendCode);
        mainMenu.addSubMenu(resendCode);
    }

    @Override
    protected CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return oasisApi.registerUser(accountContext, inviteCode);
    }

    @Override
    protected CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return oasisApi
                .loginUser(accountContext);
    }


    @Override
    public BaseDepinWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {
        String token = accountContext.getParam("token");
        if (token == null) {
            throw new IllegalArgumentException("token不能为空");
        }

        accountContext.setConnectUrl(getBotConfig().getWsBaseUrl() + "?token=" + token + "&version=0.1.20&platform=extension");

        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.forEach(httpHeaders::set);

        httpHeaders.set("Host", "ws.oasis.ai");
        httpHeaders.set("Origin", "chrome-extension://knhbjeinoabfecakfppapfgdhcpnekmm");
        httpHeaders.set("Upgrade", "websocket");
        httpHeaders.set("connection", "Upgrade");

        SimpleDepinWSClient simpleDepinWSClient = new SimpleDepinWSClient(this, accountContext);
        simpleDepinWSClient.setHeaders(httpHeaders);
        return simpleDepinWSClient;
    }


    @Override
    public void whenAccountClientStatusChange(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, WebsocketClientStatus clientStatus) {
        AccountContext accountContext = depinWSClient.getAccountContext();

        String printPrefix = String.format("账户[%s]-token[%s...]-proxy[%s]",
                accountContext.getName(), accountContext.getParam(TOKEN_KEY), accountContext.getProxy().getAddressStr()
        );

        switch (clientStatus) {
            case RUNNING -> {
                log.info("{} ws连接成功，开始挖取积分", printPrefix);
                //Step 1 发送机器信息
                sendSystemData(printPrefix, depinWSClient);


                //Step 2 主动发一次心跳
                depinWSClient.sendMessage(getHeartbeatMessage(depinWSClient));
            }
            case SHUTDOWN -> {
                log.info("[{}] ws连接已断开", printPrefix);
            }
        }
    }

    @Override
    public void whenAccountReceiveResponse(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, Object id, JSONObject response) {
        AccountContext accountContext = depinWSClient.getAccountContext();

        String token = accountContext.getParam(TOKEN_KEY);
        String printPrefix = String.format("账户[%s]-token[%s...]-proxy[%s]",
                accountContext.getName(), token.substring(0, Math.min(20, token.length())), accountContext.getProxy().getAddressStr()
        );

        log.debug("账户[{}]收到消息[{}]", printPrefix, response);

        switch (response.getString("type")) {
            case "serverMetrics" -> {
                JSONObject data = response.getJSONObject("data");
                String creditsEarned = data.getString("creditsEarned");
                log.info("{} ping[{}]成功, token[{}]-总运行时间[{}]秒-总积分[{}]",
                        printPrefix,
                        id,
                        data.getString("token"),
                        data.getString("totalUptime"),
                        creditsEarned
                );
                accountContext.getConnectStatusInfo().getErrorHeartBeat().decrementAndGet();

                RewordInfo rewordInfo = accountContext.getRewordInfo();
                rewordInfo.setTotalPoints(Double.parseDouble(creditsEarned));
            }
            case "acknowledged" -> {
                log.warn("系统更新:[{}]", response);
                depinWSClient.shutdown();
            }
            case "updateSchedule" -> {
                log.info("{}, update schedule", response);
            }
            case "error" -> {
                if (response.getJSONObject("data").getString("code").equals("invalid_token")) {
                    log.warn("{} 需要发送机器信息, {}", printPrefix, response);

                    sendSystemData(printPrefix, depinWSClient);
                }
            }
            default -> {
                log.warn("{} 收到未知消息[{}]", printPrefix, response);
            }
        }
    }


    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, JSONObject message) {
    }

    @Override
    public JSONObject getHeartbeatMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient) {
        AccountContext accountContext = depinWSClient.getAccountContext();

        log.info("账户[{}]发送心跳", accountContext.getClientAccount().getName());

        // 定时发送心跳
        JSONObject pingFrame = new JSONObject();
        pingFrame.put("id", SystemInfo.INSTANCE.getRandomId(26));
        pingFrame.put("type", "heartbeat");

        JSONObject data = new JSONObject();
        data.put("version", "0.1.7");
        data.put("mostRecentModel", "unknown");
        data.put("status", "active");

        pingFrame.put("data", data);

        accountContext.getConnectStatusInfo().getHeartBeat().incrementAndGet();
        accountContext.getConnectStatusInfo().getErrorHeartBeat().incrementAndGet();
        return pingFrame;
    }

    /**
     * 重发验证邮件
     *
     * @return 打印的字符串
     */
    private String resendCode() {
        List<CompletableFuture<Void>> futures = getAccounts().stream().map(accountContext -> {
            String email = accountContext.getClientAccount().getEmail();

            return oasisApi
                    .resendCode(accountContext)
                    .thenAccept(success -> {
                        if (success) {
                            log.info("重发邮件[{}]成功", email);
                        }
                    }).exceptionally(throwable -> {
                        log.error("邮箱[{}]重发验证邮件失败, {}", email, throwable.getMessage());
                        return null;
                    });
        }).toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            return "等待token获取完成发生错误，" + e.getMessage();
        }

        return "token获取完成，共:" + getAccounts().size() + "个账户";
    }

    /**
     * 创建随机的系统数据
     *
     */
    private static void sendSystemData(String printPrefix, BaseDepinWSClient<JSONObject, JSONObject> depinWSClient) {
        JSONObject systemData = new JSONObject();

        systemData.put("id", SystemInfo.INSTANCE.getRandomId(26));
        systemData.put("type", "system");

        JSONObject data = new JSONObject();
        data.put("gpuInfo", SystemInfo.INSTANCE.generateRandomGpuInfo());
        data.put("cpuInfo", SystemInfo.INSTANCE.generateRandomCpuInfo());
        data.put("memoryInfo", SystemInfo.INSTANCE.generateRandomMemoryInfo());
        data.put("machineId", SystemInfo.INSTANCE.getRandomId(32).toLowerCase());
        data.put("operatingSystem", SystemInfo.INSTANCE.generateRandomOperatingSystem());

        systemData.put("data", data);

        log.info("{} 发送系统信息", printPrefix);
        depinWSClient.sendMessage(systemData);
    }


    public static void main(String[] args) throws DepinBotStartException {
        OasisDepinBot oasisDepinBot = new OasisDepinBot("oasis.yaml");
        oasisDepinBot.init();
        ;
        oasisDepinBot.start();
    }
}
