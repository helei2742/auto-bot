package cn.com.helei.DepinBot.core.bot;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.dto.account.AccountPrintDto;
import cn.com.helei.DepinBot.core.dto.RewordInfo;
import cn.com.helei.DepinBot.core.pool.env.BrowserEnv;
import cn.com.helei.DepinBot.core.exception.DepinBotInitException;
import cn.com.helei.DepinBot.core.exception.DepinBotStatusException;
import cn.com.helei.DepinBot.core.netty.constants.WebsocketClientStatus;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import cn.com.helei.DepinBot.core.supporter.persistence.AccountPersistenceManager;
import cn.com.helei.DepinBot.core.util.NamedThreadFactory;
import cn.com.helei.DepinBot.core.util.table.CommandLineTablePrintHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
        import java.util.function.Function;

@Slf4j
public abstract class AccountAutoManageDepinBot<Req, Resp> extends AbstractDepinBot<Req, Resp> {
    /**
     * 异步操作线程池
     */
    private final ExecutorService executorService;

    /**
     * 账户客户端
     */
    private final ConcurrentMap<AccountContext, BaseDepinWSClient<Req, Resp>> accountWSClientMap = new ConcurrentHashMap<>();

    /**
     * 持久化管理器
     */
    private final AccountPersistenceManager persistenceManager = new AccountPersistenceManager();

    /**
     * 账号列表
     */
    @Getter
    private final List<AccountContext> accounts = new ArrayList<>();


    public AccountAutoManageDepinBot(BaseDepinBotConfig baseDepinBotConfig) {
        super(baseDepinBotConfig);


        this.executorService = Executors
                .newThreadPerTaskExecutor(new NamedThreadFactory(baseDepinBotConfig.getName() + "-account"));
    }

    @Override
    protected void doInit() throws DepinBotInitException {
        // Step 1 初始化保存的线程
        persistenceManager.init();

        // Step 2 初始化账户
        initAccounts();
    }


    /**
     * 初始化账号方法
     */
    private void initAccounts() throws DepinBotInitException {
        try {
            // Step 1 获取持久化的
            Map<Integer, AccountContext> accountContextMap = persistenceManager.loadAccountContexts();

            // Step 2 没有保存的数据，加载新的
            List<AccountContext> accountContexts;
            if (accountContextMap == null || accountContextMap.isEmpty()) {
                log.info("bot[{}]加载新账户数据", getBaseDepinBotConfig().getName());
                // Step 2.1 加载新的
                accountContexts = loadNewAccountContexts();

                // Step 2.2 持久化
                persistenceManager.persistenceAccountContexts(accountContexts);
            } else {
                log.info("bot[{}]使用历史账户数据", getBaseDepinBotConfig().getName());
                accountContexts = new ArrayList<>(accountContextMap.values());
            }

            // Step 3 加载到bot
            registerAccountsInBot(accountContexts, AccountPersistenceManager::getAccountContextPersistencePath);

            accounts.addAll(accountContexts);
        } catch (Exception e) {
            throw new DepinBotInitException("初始化账户发生错误", e);
        }
    }

    /**
     * 将账户加载到bot， 会注册监听，当属性发生改变时自动刷入磁盘
     *
     * @param accountContexts accountContexts
     */
    private void registerAccountsInBot(List<AccountContext> accountContexts, Function<AccountContext, String> getSavePath) {
        persistenceManager.registerPersistenceListener(accountContexts, getSavePath);
    }


    /**
     * 加载新的账户上下文列表，从配置文件中
     *
     * @return List<AccountContext>
     */
    private List<AccountContext> loadNewAccountContexts() {
        // Step 1 初始化账号

        List<AccountContext> newAccountContexts = new ArrayList<>();

        List<AccountContext> noProxyIds = new ArrayList<>();
        List<AccountContext> noBrowserEnvIds = new ArrayList<>();

        getAccountPool()
                .getAllItem()
                .forEach(depinClientAccount -> {
                    AccountContext accountContext = AccountContext.builder()
                            .clientAccount(depinClientAccount).build();

                    Integer id = depinClientAccount.getId();

                    // 账号没有配置代
                    if (depinClientAccount.getProxyId() == null) {
                        noProxyIds.add(accountContext);
                    } else {
                        accountContext.setProxy(getProxyPool().getItem(depinClientAccount.getProxyId()));
                    }

                    // 账号没有配置浏览器环境
                    if (depinClientAccount.getBrowserEnvId() == null) {
                        noBrowserEnvIds.add(accountContext);
                    } else {
                        accountContext.setBrowserEnv(getBrowserEnvPool().getItem(depinClientAccount.getBrowserEnvId()));
                    }

                    newAccountContexts.add(accountContext);
                });

        // Step 2 账号没代理的尝试给他设置代理
        if (!noProxyIds.isEmpty()) {
            log.warn("以下账号没有配置代理，将随机选择一个代理进行使用");
            List<NetworkProxy> lessUsedProxy = getProxyPool().getLessUsedItem(noProxyIds.size());
            for (int i = 0; i < noProxyIds.size(); i++) {
                AccountContext accountContext = noProxyIds.get(i);

                NetworkProxy proxy = lessUsedProxy.get(i);
                accountContext.setProxy(proxy);

                log.warn("账号:{},将使用代理:{}", accountContext.getName(), proxy);
            }
        }

        // Step 3 账号没浏览器环境的尝试给他设置浏览器环境
        if (!noBrowserEnvIds.isEmpty()) {
            log.warn("以下账号没有配置浏览器环境，将随机选择一个浏览器环境使用");
            List<BrowserEnv> lessUsedBrowserEnv = getBrowserEnvPool().getLessUsedItem(noBrowserEnvIds.size());
            for (int i = 0; i < noBrowserEnvIds.size(); i++) {
                AccountContext accountContext = noBrowserEnvIds.get(i);

                BrowserEnv browserEnv = lessUsedBrowserEnv.get(i);
                accountContext.setBrowserEnv(browserEnv);

                log.warn("账号:{},将使用浏览器环境:{}", accountContext.getName(), browserEnv);
            }
        }

        return newAccountContexts;
    }


    /**
     * 所有账户建立连接
     *
     * @return CompletableFuture<Void>
     */
    protected CompletableFuture<Void> connectAllAccount() {
        return CompletableFuture.runAsync(() -> {
            //Step 1 遍历账户
            List<CompletableFuture<Void>> connectFutures = accounts.stream()
                    .map(accountContext -> {
                        // Step 2 根据账户获取ws client
                        BaseDepinWSClient<Req, Resp> depinWSClient = accountWSClientMap.compute(accountContext, (k, v) -> {
                            // 没有创建过，或被关闭，创建新的
                            if (v == null || v.getClientStatus().equals(WebsocketClientStatus.SHUTDOWN)) {
                                v = buildAccountWSClient(accountContext);
                            }

                            return v;
                        });

                        String accountName = accountContext.getClientAccount().getName();

                        //Step 3 建立连接
                        WebsocketClientStatus clientStatus = depinWSClient.getClientStatus();
                        return switch (clientStatus) {
                            case NEW, STOP:  // 新创建，停止状态，需要建立连接
                                yield depinWSClient
                                        .connect()
                                        .thenAcceptAsync(success -> {
                                            try {
                                                whenAccountConnected(depinWSClient, success);
                                            } catch (Exception e) {
                                                log.error("账户[{}]-连接完成后执行回调发生错误", accountName, e);
                                            }
                                        }, executorService)
                                        .exceptionallyAsync(throwable -> {
                                            log.error("账户[{}]连接失败, ", accountName,
                                                    throwable);
                                            return null;
                                        }, executorService);
                            case STARTING, RUNNING: // 正在建立连接，直接返回
                                CompletableFuture.completedFuture(null);
                            case SHUTDOWN: // 被禁止使用，抛出异常
                                throw new DepinBotStatusException("cannot start ws client when it shutdown, " + accountName);
                        };
                    })
                    .toList();

            //Step 4 等所有账户连接建立完成
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
            BrowserEnv browserEnv = accountContext.getBrowserEnv();
            return AccountPrintDto
                    .builder()
                    .id(accountContext.getClientAccount().getId())
                    .name(accountContext.getName())
                    .proxyInfo(proxy.getId() + "-" + proxy.getAddress())
                    .browserEnvInfo(String.valueOf(browserEnv == null ? "NO_ENV" : browserEnv.getId()))
                    .signUp(accountContext.getClientAccount().getSignUp())
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
