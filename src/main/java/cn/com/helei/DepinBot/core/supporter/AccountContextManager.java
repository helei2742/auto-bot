package cn.com.helei.DepinBot.core.supporter;

import cn.com.helei.DepinBot.core.AbstractDepinWSClient;
import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.CommandLineDepinBot;
import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.dto.AccountPrintDto;
import cn.com.helei.DepinBot.core.dto.DepinClientAccount;
import cn.com.helei.DepinBot.core.dto.RewordInfo;
import cn.com.helei.DepinBot.core.env.BrowserEnvPool;
import cn.com.helei.DepinBot.core.exception.DepinBotInitException;
import cn.com.helei.DepinBot.core.network.NetworkProxy;
import cn.com.helei.DepinBot.core.network.NetworkProxyPool;
import cn.com.helei.DepinBot.core.util.NamedThreadFactory;
import cn.com.helei.DepinBot.core.util.table.CommandLineTablePrintHelper;
import com.jakewharton.fliptables.FlipTable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * 账户上下文管理器
 */
@Slf4j
public class AccountContextManager {

    /**
     * 异步操作线程池
     */
    private final ExecutorService executorService;

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
    private final Function<AccountContext, AbstractDepinWSClient<?, ?>> clientCreator;

    /**
     * 账户成功链接后的回调
     */
    private final BiConsumer<AccountContext, Boolean> accountConnectedHandler;

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
        this.accountConnectedHandler = commandLineDepinBot::whenAccountConnected;

        this.executorService = Executors
                .newThreadPerTaskExecutor(new NamedThreadFactory(baseDepinBotConfig.getName() + "-account"));
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
                                    .whenCompleteAsync((success, throwable) -> {
                                        if (throwable != null || !success) {
                                            log.error("账户[{}]连接失败, ", accountContext.getClientAccount().getName(), throwable);
                                        }

                                        accountConnectedHandler.accept(accountContext, success);
                                    }, executorService)
                    )
                    .toList();

            try {
                CompletableFuture
                        .allOf(connectFutures.toArray(new CompletableFuture[0]))
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("账户建立连接发生异常", e);
            }
        }, executorService);
    }

    /**
     * 打印账号列表
     *
     * @return String
     */
    public String printAccountList() {
        List<AccountPrintDto> list = accounts.stream().map(accountContext -> {
            NetworkProxy proxy = accountContext.getProxy();
            return AccountPrintDto
                    .builder()
                    .name(accountContext.getClientAccount().getName())
                    .proxyInfo(proxy.getId() + "-" + proxy.getAddress())
                    .browserEnvInfo(String.valueOf(accountContext.getBrowserEnv().getId()))
                    .usable(accountContext.isUsable())
                    .startDateTime(accountContext.getConnectStatusInfo().getStartDateTime())
                    .updateDateTime(accountContext.getConnectStatusInfo().getUpdateDateTime())
                    .heartBeatCount(accountContext.getConnectStatusInfo().getHeartBeatCount().get())
                    .connectStatus(accountContext.getConnectStatusInfo().getConnectStatus())
                    .build();
        }).toList();

        return "账号列表:\n" +
                CommandLineTablePrintHelper.generateTableString(list, AccountPrintDto.class) +
                "\n";
    }

    /**
     * 打印账号收益
     *
     * @return String
     */
    public String printAccountReward() {
        StringBuilder sb = new StringBuilder();

        List<RewordInfo> list = accounts.stream().map(AccountContext::getRewordInfo).toList();

        sb.append("收益列表:\n")
                .append(CommandLineTablePrintHelper.generateTableString(list, RewordInfo.class))
                .append("\n");

        return sb.toString();
    }
}
