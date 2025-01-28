package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.BaseDepinBotConfig;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.dto.account.AccountPrintDto;
import cn.com.helei.bot.core.dto.RewordInfo;
import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.exception.RewardQueryException;
import cn.com.helei.bot.core.pool.env.BrowserEnv;
import cn.com.helei.bot.core.exception.DepinBotInitException;
import cn.com.helei.bot.core.pool.env.BrowserEnvPool;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.com.helei.bot.core.pool.network.NetworkProxyPool;
import cn.com.helei.bot.core.pool.twitter.TwitterPool;
import cn.com.helei.bot.core.supporter.persistence.AccountPersistenceManager;
import cn.com.helei.bot.core.util.ClosableTimerTask;
import cn.com.helei.bot.core.util.table.CommandLineTablePrintHelper;
import cn.hutool.core.util.BooleanUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
        import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class AccountAutoManageDepinBot extends AbstractDepinBot {

    /**
     * 持久化管理器
     */
    private final AccountPersistenceManager persistenceManager;

    /**
     * 账号列表
     */
    @Getter
    private final List<AccountContext> accounts = new ArrayList<>();

    /**
     * 是否允许账户收益查询
     */
    private final AtomicBoolean isRunningAccountRewardQuery = new AtomicBoolean(true);


    /**
     * 存放账户对应的addTimer添加的任务
     */
    private final Map<AccountContext, Set<ClosableTimerTask>> accountTimerTaskMap;

    /**
     * task 任务并发控制
     */
    private final Semaphore taskSyncController;



    public AccountAutoManageDepinBot(BaseDepinBotConfig baseDepinBotConfig) {
        super(baseDepinBotConfig);

        this.persistenceManager = new AccountPersistenceManager(baseDepinBotConfig.getName());
        this.accountTimerTaskMap = new ConcurrentHashMap<>();
        this.taskSyncController = new Semaphore(baseDepinBotConfig.getConcurrentCount());
    }

    @Override
    protected void doInit() throws DepinBotInitException {
        // Step 1 初始化保存的线程
        persistenceManager.init();

        // Step 2 初始化账户
        initAccounts();
    }


    @Override
    public void start() throws DepinBotStartException {
        super.start();

        // 启动奖励查询任务
        if (BooleanUtil.isTrue(getBaseDepinBotConfig().getIsAccountRewardAutoRefresh())) {
            startAccountRewardQueryTask();
        }
    }

    /**
     * 添加定时任务,closableTimerTask执行run方法放回true会继续执行， 返回false则会跳出循环
     *
     * @param taskLogic taskLogic
     * @param delay    delay
     * @param timeUnit timeUnit
     */
    public ClosableTimerTask addTimer(
            Supplier<Boolean> taskLogic,
            long delay,
            TimeUnit timeUnit,
            AccountContext timerOwner
    ) {
        ClosableTimerTask closableTimerTask = new ClosableTimerTask(taskLogic);

        accountTimerTaskMap.compute(timerOwner, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(closableTimerTask);
            return v;
        });

        getExecutorService().execute(() -> {
            while (true) {
                try {
                    taskSyncController.acquire();

                    if (closableTimerTask.isRunning()) {
                        closableTimerTask.setRunning(closableTimerTask.getTask().get());
                    }

                    if (!closableTimerTask.isRunning()) {
                        // 运行完毕后移除
                        accountTimerTaskMap.get(timerOwner).remove(closableTimerTask);
                        break;
                    }

                    timeUnit.sleep(delay);
                } catch (Exception e) {
                    log.error("定时任务执行失败", e);
                    // 异常退出后移除
                    accountTimerTaskMap.get(timerOwner).remove(closableTimerTask);
                    break;
                } finally {
                    taskSyncController.release();
                }
            }
        });

        return closableTimerTask;
    }

    /**
     * 开启账户奖励查询任务
     */
    private void startAccountRewardQueryTask() {
        getExecutorService().execute(() -> {
            while (isRunningAccountRewardQuery.get()) {
                List<AccountContext> accounts = getAccounts();

                List<CompletableFuture<Boolean>> futures = accounts.stream().map(accountContext -> {
                    try {
                        return updateAccountRewordInfo(accountContext);
                    } catch (Exception e) {
                        throw new RewardQueryException(e);
                    }
                }).toList();

                for (int i = 0; i < futures.size(); i++) {
                    try {
                        futures.get(i).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("查询账户[" + accounts.get(i).getName() + "]奖励失败", e.getMessage());
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(getBaseDepinBotConfig().getAccountRewardRefreshIntervalSeconds());
                } catch (InterruptedException e) {
                    log.error("等待执行账户查询时发生异常", e);
                }
            }
        });
    }


    /**
     * 更新账户奖励信息
     *
     * @param accountContext accountContext
     * @return CompletableFuture<Boolean>
     */
    protected abstract CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext);


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
            registerAccountsInBot(accountContexts,
                    accountContext ->  AccountPersistenceManager.getAccountContextPersistencePath(getBaseDepinBotConfig().getName(), accountContext));

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

        NetworkProxyPool proxyPool = getProxyPool();
        BrowserEnvPool browserEnvPool = getBrowserEnvPool();
        TwitterPool twitterPool = getTwitterPool();

        getAccountPool()
                .getAllItem()
                .forEach(depinClientAccount -> {
                    AccountContext accountContext = AccountContext.builder()
                            .clientAccount(depinClientAccount).build();

                    Integer id = depinClientAccount.getId();


                    // 账号没有配置代
                    if (depinClientAccount.getProxyId() != null) {
                        accountContext.setProxy(proxyPool.getItem(depinClientAccount.getProxyId()));
                    } else if (id < proxyPool.size()){
                        accountContext.getClientAccount().setProxyId(id);
                        accountContext.setProxy(proxyPool.getItem(id));
                    } else {
                        noProxyIds.add(accountContext);
                    }

                    // 账号没有配置浏览器环境
                    if (depinClientAccount.getBrowserEnvId() != null) {
                        accountContext.setBrowserEnv(browserEnvPool.getItem(depinClientAccount.getBrowserEnvId()));
                    } else if (id < browserEnvPool.size()){
                        accountContext.getClientAccount().setBrowserEnvId(id);
                        accountContext.setBrowserEnv(browserEnvPool.getItem(id));
                    } else {
                        noBrowserEnvIds.add(accountContext);
                    }

                    // 添加推特
                    if (depinClientAccount.getTwitterId() != null) {
                        accountContext.setTwitter(twitterPool.getItem(depinClientAccount.getTwitterId()));
                    } else if (id < twitterPool.size()){
                        accountContext.getClientAccount().setTwitterId(id);
                        accountContext.setTwitter(twitterPool.getItem(id));
                    }


                    newAccountContexts.add(accountContext);
                });

        // Step 2 账号没代理的尝试给他设置代理
        if (!noProxyIds.isEmpty()) {
            log.warn("以下账号没有配置代理，将随机选择一个代理进行使用");
            List<NetworkProxy> lessUsedProxy = proxyPool.getLessUsedItem(noProxyIds.size());
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
            List<BrowserEnv> lessUsedBrowserEnv = browserEnvPool.getLessUsedItem(noBrowserEnvIds.size());
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
                    .build();
        }).toList();

        return "账号列表:\n" +
                CommandLineTablePrintHelper.generateTableString(list, AccountPrintDto.class) +
                "\n";
    }

    /**
     * 打印账户连接情况
     *
     * @return String
     */
    public String printAccountConnectStatusList() {
        List<ConnectStatusInfo> list = accounts.stream().map(AccountContext::getConnectStatusInfo).toList();

        return "账号链接状态列表:\n" +
                CommandLineTablePrintHelper.generateTableString(list, ConnectStatusInfo.class) +
                "\n";
    }

    /**
     * 打印账号收益
     *
     * @return String
     */
    public String printAccountReward() {
        StringBuilder sb = new StringBuilder();

        List<RewordInfo> list = accounts.stream()
                .map(accountContext -> accountContext.getRewordInfo().newInstance()).toList();

        sb.append("收益列表:\n")
                .append(CommandLineTablePrintHelper.generateTableString(list, RewordInfo.class))
                .append("\n");

        return sb.toString();
    }


    /**
     * 去除账户的所有计时任务
     *
     * @param accountContext    accountContext
     */
    public void removeAccountTimer(AccountContext accountContext) {
        accountTimerTaskMap.compute(accountContext, (k,v)->{
            if (v != null) {
                v.forEach(task->task.setRunning(false));
                v.removeIf(task->true);
            }
            return v;
        });
    }
}
