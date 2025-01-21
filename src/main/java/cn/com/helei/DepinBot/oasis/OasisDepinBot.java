package cn.com.helei.DepinBot.oasis;

import cn.com.helei.DepinBot.core.AbstractDepinWSClient;
import cn.com.helei.DepinBot.core.CommandLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.network.NetworkProxy;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

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


        return main.addSubMenu(register).addSubMenu(takeToken).addSubMenu(resendCode);
    }



    @Override
    public AbstractDepinWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {
        return null;
    }

    @Override
    public void whenAccountConnected(AccountContext accountContext, Boolean success) {

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
        List<CompletableFuture<Void>> futureList = getAccountContextManager().getAccounts().stream().map(accountContext -> {
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

        return "注册完成,成功注册" + successCount.get() + "个账户" + "共:" + getAccountContextManager().getAccounts().size() + "个账户";
    }


    /**
     * 登录获取token
     *
     * @return token
     */
    private String loginAndTakeToken() {

        List<CompletableFuture<Void>> futures = getAccountContextManager().getAccounts().stream().map(accountContext -> {
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

        return "token获取完成，共:" + getAccountContextManager().getAccounts().size() + "个账户";
    }

    private String resendCode() {
        List<CompletableFuture<Void>> futures = getAccountContextManager().getAccounts().stream().map(accountContext -> {
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

        return "token获取完成，共:" + getAccountContextManager().getAccounts().size() + "个账户";
    }

}
