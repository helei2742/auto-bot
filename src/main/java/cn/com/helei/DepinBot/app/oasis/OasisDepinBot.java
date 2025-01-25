package cn.com.helei.DepinBot.app.oasis;

import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.SimpleDepinWSClient;
import cn.com.helei.DepinBot.core.bot.WSMenuCMDLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.commandMenu.DefaultMenuType;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.util.SystemInfo;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class OasisDepinBot extends WSMenuCMDLineDepinBot<OasisBotConfig, JSONObject, JSONObject> {


    private final OasisApi oasisApi;

    public OasisDepinBot(String oasisBotConfigPath) {
        super(OasisBotConfig.loadYamlConfig(oasisBotConfigPath));
        this.oasisApi = new OasisApi(this);
    }

    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {
        defaultMenuTypes.add(DefaultMenuType.START_ACCOUNT_CLAIM);

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
        SimpleDepinWSClient simpleDepinWSClient = new SimpleDepinWSClient(this, accountContext);
        simpleDepinWSClient.setAllIdleTimeSecond((int) getBotConfig().getAutoClaimIntervalSeconds());

        return simpleDepinWSClient;
    }


    @Override
    public void whenAccountConnected(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, Boolean success) {
        //Step 1 发送机器信息
        depinWSClient.sendMessage(generateRandomSystemData());

        //Step 2 主动发一次心跳
        depinWSClient.sendMessage(getHeartbeatMessage(depinWSClient));
    }

    @Override
    public void whenAccountReceiveResponse(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, String id, JSONObject response) {

    }


    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, JSONObject message) {
        String accountName = depinWSClient.getAccountContext().getClientAccount().getName();
        log.debug("账户[{}]收到消息[{}]", accountName, message);

        switch (message.getString("type")) {
            case "serverMetrics" -> {
                log.info("账户[{}]心跳已发送, token[{}]-总运行时间[{}]秒-总积分[{}]",
                        accountName,
                        message.getString("token"),
                        message.getString("totalUptime"),
                        message.getString("creditsEarned")
                );
            }
            case "acknowledged" -> {
                log.warn("系统更新:[{}]", message);
            }
            case "error" -> {
                if (message.getJSONObject("data").getString("code").equals("invalid_token")) {
                    log.warn("账户[{}]需要发送机器信息", accountName);
                    depinWSClient.sendMessage(generateRandomSystemData());
                }
            }
            default -> {
                log.warn("账户[{}]收到位置消息[{}]", accountName, message);
            }
        }
    }

    @Override
    public JSONObject getHeartbeatMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient) {
        log.info("账户[{}]发送心跳", depinWSClient.getAccountContext().getClientAccount().getName());

        // 定时发送心跳
        JSONObject pingFrame = new JSONObject();
        pingFrame.put("id", SystemInfo.INSTANCE.getRandomId(26));
        pingFrame.put("type", "heartbeat");

        JSONObject data = new JSONObject();
        data.put("version", "0.1.7");
        data.put("mostRecentModel", "unknown");
        data.put("status", "active");

        pingFrame.put("data", data);

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
     * @return JSONObject
     */
    private static @NotNull JSONObject generateRandomSystemData() {
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
        return systemData;
    }
}
