package cn.com.helei.DepinBot.oasis;

import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.bot.CommandLineDepinBot;
import cn.com.helei.DepinBot.core.SimpleDepinWSClient;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import cn.com.helei.DepinBot.core.util.SystemInfo;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class OasisDepinBot extends CommandLineDepinBot<JSONObject, JSONObject> {

    private final OasisApi oasisApi;

    private final Semaphore concurrentSemaphore;

    public OasisDepinBot(String oasisBotConfigPath) {
        super(OasisBotConfig.loadYamlConfig(oasisBotConfigPath));
        this.oasisApi = new OasisApi(getExecutorService());
        this.concurrentSemaphore = new Semaphore(getBaseDepinBotConfig().getConcurrentCount());
    }

    @Override
    protected CommandMenuNode buildMenuNode() {
        CommandMenuNode main = new CommandMenuNode("主菜单", "欢迎使用机器人", null);

        CommandMenuNode register = new CommandMenuNode(true, "账户注册",
                "开始批量注册账号", this::registerAccount);

        CommandMenuNode takeToken = new CommandMenuNode(true, "获取token",
                "开始获取token", this::loginAndTakeToken);

        CommandMenuNode resendCode = new CommandMenuNode(true, "重发验证邮件",
                "开始重发验证邮件", this::resendCode);



        return main
                .addSubMenu(register)
                .addSubMenu(takeToken)
                .addSubMenu(resendCode);
    }


    @Override
    public BaseDepinWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {
        return new SimpleDepinWSClient(this, accountContext);
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
        pingFrame.put("type","heartbeat");

        JSONObject data = new JSONObject();
        data.put("version", "0.1.7");
        data.put("mostRecentModel", "unknown");
        data.put("status", "active");

        pingFrame.put("data", data);

        return pingFrame;
    }


    /**
     * 注册
     *
     * @return 注册
     */
    private String registerAccount() {
        OasisBotConfig oasisBotConfig = (OasisBotConfig) getBaseDepinBotConfig();
        String inviteCode = oasisBotConfig.getInviteCode();

        log.info("开始注册账户");
        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futureList = getAccounts().stream().map(accountContext -> {
            try {
                concurrentSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String email = accountContext.getClientAccount().getEmail();

            NetworkProxy proxy = accountContext.getProxy();

            log.info("注册[{}]..使用邀请码[{}]..代理[{}-{}]", email, inviteCode, proxy.getId(), proxy.getAddress());
            return oasisApi.registerUser(proxy, email, accountContext.getClientAccount().getPassword(), inviteCode)
                    .exceptionally(throwable -> {
                        log.error("注册[{}]时发生异常", email, throwable);
                        return false;
                    })
                    .thenAcceptAsync(success -> {
                        if (success) {
                            accountContext.setUsable(true);
                            successCount.getAndIncrement();
                        } else {
                            accountContext.setUsable(false);
                        }
                    }).whenCompleteAsync((Void, throwable) -> {
                        concurrentSemaphore.release();
                    });
        }).toList();


        try {
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            return "等待账户注册完成时发生错误" + e.getMessage();
        }

        return "注册完成,成功注册" + successCount.get() + "个账户" + "共:" + getAccounts().size() + "个账户";
    }


    /**
     * 登录获取token
     *
     * @return token
     */
    private String loginAndTakeToken() {

        List<CompletableFuture<Void>> futures = getAccounts().stream().map(accountContext -> {
            try {
                concurrentSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String email = accountContext.getClientAccount().getEmail();
            NetworkProxy proxy = accountContext.getProxy();

            return oasisApi
                    .loginUser(proxy, email, accountContext.getClientAccount().getPassword())
                    .thenAccept(token -> {
                        if (StrUtil.isNotBlank(token)) {
                            log.info("邮箱[{}]登录成功，token[{}]", email, token);

                            accountContext.setParam("token", token);
                        }
                    }).exceptionally(throwable -> {
                        log.error("邮箱[{}]登录失败, {}", email, throwable.getMessage());
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
     * 重发验证邮件
     *
     * @return 打印的字符串
     */
    private String resendCode() {
        List<CompletableFuture<Void>> futures = getAccounts().stream().map(accountContext -> {
            try {
                concurrentSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String email = accountContext.getClientAccount().getEmail();
            NetworkProxy proxy = accountContext.getProxy();

            return oasisApi
                    .resendCode(proxy, email)
                    .thenAccept(success -> {
                        if (success) {
                            log.info("重发邮件[{}]成功", email);
                        }
                        concurrentSemaphore.release();
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

        return "token获取完成，共:" +  getAccounts().size() + "个账户";
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
