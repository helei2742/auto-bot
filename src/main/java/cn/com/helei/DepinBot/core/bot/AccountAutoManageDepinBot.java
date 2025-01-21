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
import cn.com.helei.DepinBot.core.util.NamedThreadFactory;
import cn.com.helei.DepinBot.core.util.table.CommandLineTablePrintHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
        import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AccountAutoManageDepinBot<Req, Resp> extends AbstractDepinBot<Req, Resp> {
    /**
     * 异步操作线程池
     */
    private final ExecutorService executorService;

    /**
     * 是否开始过链接所有账号
     */
    private final AtomicBoolean isStartAccountConnected = new AtomicBoolean(false);

    /**
     * 账户客户端
     */
    private final ConcurrentMap<AccountContext, BaseDepinWSClient<Req, Resp>> accountWSClientMap = new ConcurrentHashMap<>();

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
        initAccounts();
    }


    /**
     * 初始化账号方法
     */
    public void initAccounts() throws DepinBotInitException {
        try {
            //Step 1 初始化账号
            List<AccountContext> notUsableAccounts = new ArrayList<>();
            getBaseDepinBotConfig()
                    .getAccountList()
                    .forEach(depinClientAccount -> {
                        AccountContext.AccountContextBuilder builder = AccountContext.builder().clientAccount(depinClientAccount);

                        //账号没有配置代理，则将其设置为不可用
                        if (depinClientAccount.getProxyId() == null) {
                            builder.usable(false);
                        } else {
                            builder.browserEnv(getBrowserEnvPool().getItem(depinClientAccount.getBrowserEnvId()))
                                    .usable(true)
                                    .proxy(getProxyPool().getItem(depinClientAccount.getProxyId()))
                                    .build();
                        }

                        AccountContext build = builder.build();
                        if (!build.isUsable()) notUsableAccounts.add(build);

                        accounts.add(build);
                    });

            //Step 2 账号没代理的尝试给他设置代理
            if (!notUsableAccounts.isEmpty()) {
                log.warn("以下账号没有配置代理，将随机选择一个代理进行使用");
                List<NetworkProxy> lessUsedProxy = getProxyPool().getLessUsedItem(notUsableAccounts.size());
                for (int i = 0; i < notUsableAccounts.size(); i++) {
                    notUsableAccounts.get(i).setProxy(lessUsedProxy.get(i));
                    notUsableAccounts.get(i).setUsable(true);
                    log.warn("账号:{},将使用代理:{}", notUsableAccounts.get(i).getName(), lessUsedProxy.get(i));
                }
            }
        } catch (Exception e) {
            throw new DepinBotInitException("初始化账户发生错误", e);
        }
    }

    /**
     * 开始所有账户的连接
     *
     * @return String 打印的消息
     */
    public String startAccountDepinClient() {
        if (isStartAccountConnected.compareAndSet(false, true)) {
            allAccountConnectExecute()
                    .exceptionally(throwable -> {
                        log.error("开始所有账户连接时发生异常", throwable);
                        return null;
                    });
            return "已开始账号链接任务";
        }

        return "已提交过建立连接任务";
    }

    /**
     * 所有账户建立连接
     *
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> allAccountConnectExecute() {
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
                                throw new DepinBotStatusException("cannot start ws client when it shutdown, "  + accountName);
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
                    .name(accountContext.getClientAccount().getName())
                    .proxyInfo(proxy.getId() + "-" + proxy.getAddress())
                    .browserEnvInfo(String.valueOf(browserEnv == null ? "NO_ENV" : browserEnv.getId()))
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
