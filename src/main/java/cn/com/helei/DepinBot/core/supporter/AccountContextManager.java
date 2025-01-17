package cn.com.helei.DepinBot.core.supporter;

import cn.com.helei.DepinBot.core.AbstractDepinWSClient;
import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.CommandLineDepinBot;
import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.dto.DepinClientAccount;
import cn.com.helei.DepinBot.core.env.BrowserEnvPool;
import cn.com.helei.DepinBot.core.exception.DepinBotInitException;
import cn.com.helei.DepinBot.core.network.NetworkProxy;
import cn.com.helei.DepinBot.core.network.NetworkProxyPool;
import com.jakewharton.fliptables.FlipTable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Slf4j
public class AccountContextManager {

    private final String[] ACCOUNT_PRINT_TABLE_HEADER = {"row", "name", "proxy", "browser env","usable", "start time", "update time", "heartbeat", "status"};

    /**
     * 代理池
     */
    private final NetworkProxyPool proxyPool;

    /**
     * 浏览器环境池
     */
    private final BrowserEnvPool browserEnvPool;

    /**
     * 配置
     */
    private final BaseDepinBotConfig baseDepinBotConfig;

    /**
     * 创建AbstractDepinWSClient的Function
     */
    private final Function<AccountContext, AbstractDepinWSClient<?,?>> clientCreator;

    /**
     * 账号列表
     */
    @Getter
    private final List<AccountContext> accounts = new ArrayList<>();


    public <Req, Resp> AccountContextManager(CommandLineDepinBot<Req, Resp> commandLineDepinBot) {
        this.proxyPool = commandLineDepinBot.getProxyPool();
        this.browserEnvPool = commandLineDepinBot.getBrowserEnvPool();
        this.baseDepinBotConfig = commandLineDepinBot.getBaseDepinBotConfig();

        this.clientCreator = commandLineDepinBot::buildAccountWSClient;
    }


    /**
     * 初始化账号方法
     */
    public void initAccounts() throws DepinBotInitException {
        try {
            //Step 1 初始化账号
            List<AccountContext> notUsableAccounts = new ArrayList<>();
            baseDepinBotConfig
                    .getAccountList()
                    .forEach(depinClientAccount -> {
                        AccountContext.AccountContextBuilder builder = AccountContext.builder().clientAccount(depinClientAccount);

                        //账号没有配置代理，则将其设置为不可用
                        if (depinClientAccount.getProxyId() == null) {
                            builder.usable(false);
                        } else {
                            builder.browserEnv(browserEnvPool.getBrowserEnv(depinClientAccount.getBrowserEnvId()))
                                    .usable(true)
                                    .proxy(proxyPool.getProxy(depinClientAccount.getProxyId())).build();
                        }

                        AccountContext build = builder.build();
                        if (!build.isUsable()) notUsableAccounts.add(build);

                        accounts.add(build);
                    });

            //Step 2 账号没代理的尝试给他设置代理
            if (!notUsableAccounts.isEmpty()) {
                log.warn("以下账号没有配置代理，将随机选择一个代理进行使用");
                List<NetworkProxy> lessUsedProxy = proxyPool.getLessUsedProxy(notUsableAccounts.size());
                for (int i = 0; i < notUsableAccounts.size(); i++) {
                    notUsableAccounts.get(i).setProxy(lessUsedProxy.get(i));
                    notUsableAccounts.get(i).setUsable(true);
                    log.error("账号:{},将使用代理:{}", notUsableAccounts.get(i).getClientAccount().getName(), lessUsedProxy.get(i));
                }
            }
        } catch (Exception e) {
            throw new DepinBotInitException("初始化账户发生错误", e);
        }
    }

    /**
     * 所有账户建立连接
     *
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> allAccountConnectExecute() {
        return CompletableFuture.runAsync(() -> {
            List<CompletableFuture<Boolean>> connectFutures = accounts.stream()
                    .map(accountContext ->
                            clientCreator
                                    .apply(accountContext)
                                    .connect()
                                    .exceptionally(throwable -> {
                                        log.error("账户[{}]连接失败", accountContext.getClientAccount().getName());
                                        return null;
                                    })
                    )
                    .toList();

            try {
                CompletableFuture
                        .allOf(connectFutures.toArray(new CompletableFuture[0]))
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("账户建立连接发生异常", e);
            }
        });
    }

    /**
     * 打印账号列表
     *
     * @return String
     */
    public String printAccountList() {
        StringBuilder sb = new StringBuilder();


        String[][] table = new String[accounts.size()][ACCOUNT_PRINT_TABLE_HEADER.length];

        for (int i = 0; i < accounts.size(); i++) {
            AccountContext accountContext = accounts.get(i);
            DepinClientAccount clientAccount = accountContext.getClientAccount();
            table[i] = new String[]{
                    String.valueOf(i),
                    clientAccount.getName(),
                    accountContext.getProxy().getHost() + ":" + accountContext.getProxy().getPort(),
                    String.valueOf(clientAccount.getBrowserEnvId()),
                    String.valueOf(accountContext.isUsable()),
                    String.valueOf(accountContext.getConnectStatusInfo().getStartDateTime()),
                    String.valueOf(accountContext.getConnectStatusInfo().getUpdateDateTime()),
                    String.valueOf(accountContext.getConnectStatusInfo().getHeartBeatCount()),
                    String.valueOf(accountContext.getConnectStatusInfo().getConnectStatus())
            };
        }

        sb.append("账号列表:\n").append(FlipTable.of(ACCOUNT_PRINT_TABLE_HEADER, table)).append("\n");

        return sb.toString();
    }
}
