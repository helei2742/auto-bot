package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.config.BaseDepinBotConfig;
import cn.com.helei.bot.core.config.TypedAccountConfig;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.dto.account.AccountBaseInfo;
import cn.com.helei.bot.core.exception.RewardQueryException;
import cn.com.helei.bot.core.pool.env.BrowserEnv;
import cn.com.helei.bot.core.exception.DepinBotInitException;
import cn.com.helei.bot.core.pool.env.BrowserEnvPool;
import cn.com.helei.bot.core.pool.network.AbstractProxyPool;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.com.helei.bot.core.pool.network.ProxyType;
import cn.com.helei.bot.core.pool.twitter.TwitterPool;
import cn.com.helei.bot.core.supporter.persistence.AccountPersistenceManager;
import cn.com.helei.bot.core.util.ClosableTimerTask;
import cn.com.helei.bot.core.util.FileUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class AccountAutoManageDepinBot extends AbstractDepinBot implements AccountAutoBot {

    public static final String INVITE_CODE_KEY = "inviteCode";

    public static final String REGISTER_TYPE_KEY = "registerType";

    /**
     * 持久化管理器
     */
    private final AccountPersistenceManager persistenceManager;

    /**
     * 账号列表
     */
    @Getter
    private final Map<String, List<AccountContext>> typedAccountMap = new HashMap<>();

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

    /**
     * 是否开始过链接所有账号
     */
    private final Set<String> startedAccountType = new ConcurrentHashSet<>();


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
     * @param delay     delay
     * @param timeUnit  timeUnit
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
     * 去除账户的所有计时任务
     *
     * @param accountContext accountContext
     */
    public void removeAccountTimer(AccountContext accountContext) {
        accountTimerTaskMap.compute(accountContext, (k, v) -> {
            if (v != null) {
                v.forEach(task -> task.setRunning(false));
                v.removeIf(task -> true);
            }
            return v;
        });
    }


    /**
     * 注册所有账号
     *
     * @return String
     */
    public String registerTypeAccount() {
        String type = getBaseDepinBotConfig().getConfig(REGISTER_TYPE_KEY);
        if (!getTypedAccountMap().containsKey(type)) {
            return type + " 类型账户不存在";
        }

        StringBuilder sb = new StringBuilder("已开始账户注册, type: [");

        // Step 1 遍历不同类型的账户
        List<AccountContext> accountContexts = getTypedAccountMap().get(type);

        sb.append(type).append(", ");

        // Step 2 遍历不同类型下的所有账户
        List<CompletableFuture<Boolean>> futures = accountContexts.stream()
                .map(account -> {
                    // 账户注册过，
                    if (BooleanUtil.isTrue(account.getAccountBaseInfo().getSignUp())) {
                        log.warn("[{}]账户[{}]-email[{}]注册过", type, account.getName(),
                                account.getAccountBaseInfo().getEmail());

                        return CompletableFuture.completedFuture(false);
                    } else {
                        return registerAccount(account, getBaseDepinBotConfig().getConfig(INVITE_CODE_KEY));
                    }
                }).toList();

        // Step 3 等待注册完成
        int successCount = 0;
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<Boolean> future = futures.get(i);
            AccountContext accountContext = accountContexts.get(i);

            try {
                if (future.get()) {
                    //注册成功
                    successCount++;
                    accountContext.getAccountBaseInfo().setSignUp(true);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("注册[{}]账号[{}]发生错误, {}", type, accountContext.getName(), e.getMessage());
            }
        }

        log.info("[{}] 所有账号注册完毕，[{}/{}}]", type, successCount, accountContexts.size());

        return sb.append("]").toString();
    }


    /**
     * 获取账号的token
     *
     * @return String
     */
    public String loadTypedAccountToken(String type) {
        List<AccountContext> accountContexts = typedAccountMap.get(type);

        log.info("开始获取[{}]类型账号token", type);

        List<CompletableFuture<String>> futures = accountContexts.stream()
                .map(this::requestTokenOfAccount).toList();

        int successCount = 0;
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<String> future = futures.get(i);
            AccountContext accountContext = accountContexts.get(i);
            try {
                String token = future.get();
                if (StrUtil.isNotBlank(token)) {
                    successCount++;
                    accountContext.getParams().put("token", token);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("[{}] 账号[{}]获取token发生错误, {}", type, accountContext.getName(), e.getMessage());
            }
        }

        log.info("[{}]类型账号token获取完毕, [{}/{}]", type, successCount, accountContexts.size());

        return "已开始获取账户token";
    }


    public List<AccountContext> getAccounts() {
        List<AccountContext> accountContexts = new ArrayList<>();
        for (List<AccountContext> value : getTypedAccountMap().values()) {
            accountContexts.addAll(value);
        }
        return accountContexts;
    }

    /**
     * 账号被加载后调用
     *
     * @param typedAccountMap typedAccountMap
     */
    protected void typedAccountsLoadedHandler(Map<String, List<AccountContext>> typedAccountMap) {

    }

    /**
     * 开始所有账户Claim
     *
     * @return String 打印的消息
     */
    protected String startAccountsClaim(String type, List<AccountContext> accountContexts) {
        if (startedAccountType.add(type)) {
            doAccountsClaim(type, accountContexts);
            return "已开始[" + type + "]类型账号自动收获";
        }

        return type + " 类型账户自动收获任务已开启";
    }

    /**
     * 开始账户claim
     */
    protected void doAccountsClaim(String type, List<AccountContext> accountContexts) {
        log.info("开始[{}]账户claim", type);

        accountContexts.forEach(account -> {
            account.getConnectStatusInfo().setStartDateTime(LocalDateTime.now());

            // 添加定时任务
            addTimer(
                    () -> doAccountClaim(account),
                    getBaseDepinBotConfig().getAutoClaimIntervalSeconds(),
                    TimeUnit.SECONDS,
                    account
            );
        });
    }


    /**
     * 开启账户奖励查询任务
     */
    private void startAccountRewardQueryTask() {
        getExecutorService().execute(() -> {
            while (isRunningAccountRewardQuery.get()) {
                for (Map.Entry<String, List<AccountContext>> entry : getTypedAccountMap().entrySet()) {
                    List<AccountContext> accounts = entry.getValue();

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
            }
        });
    }


    /**
     * 初始化账号方法
     */
    private void initAccounts() throws DepinBotInitException {
        try {
            // Step 1 获取持久化的
            Map<String, List<AccountContext>> typedAccountMap = persistenceManager.loadAccountContexts();

            // Step 2 没有保存的数据，加载新的
            if (typedAccountMap == null || typedAccountMap.isEmpty()) {
                log.info("bot[{}]加载新账户数据", getBaseDepinBotConfig().getName());
                // Step 2.1 加载新的
                typedAccountMap = loadNewAccountContexts();

                // Step 2.2 持久化
                persistenceManager.persistenceAccountContexts(typedAccountMap);
            } else {
                log.info("bot[{}]使用历史账户数据", getBaseDepinBotConfig().getName());
            }


            // Step 3 加载到bot
            registerAccountsInBot(typedAccountMap,
                    accountContext -> AccountPersistenceManager.getAccountContextPersistencePath(
                            getBaseDepinBotConfig().getName(), accountContext.getAccountBaseInfo().getType(), accountContext
                    )
            );

            typedAccountsLoadedHandler(typedAccountMap);

            this.typedAccountMap.putAll(typedAccountMap);
        } catch (Exception e) {
            throw new DepinBotInitException("初始化账户发生错误", e);
        }
    }


    /**
     * 将账户加载到bot， 会注册监听，当属性发生改变时自动刷入磁盘
     *
     * @param typedAccountMap typedAccountMap
     */
    private void registerAccountsInBot(Map<String, List<AccountContext>> typedAccountMap, Function<AccountContext, String> getSavePath) {
        typedAccountMap.forEach((type, list) -> {
            persistenceManager.registerPersistenceListener(list, getSavePath);
        });
    }

    /**
     * 加载新的账户上下文列表，从配置文件中
     *
     * @return Map<String, List < AccountContext>>
     */
    private Map<String, List<AccountContext>> loadNewAccountContexts() {
        // Step 0 获取配置
        BaseDepinBotConfig botConfig = getBaseDepinBotConfig();
        List<TypedAccountConfig> accountConfigs = botConfig.getAccountConfigs();

        Map<String, List<AccountContext>> typedAccountContextMap = new HashMap<>();

        // 根据配置加载账号
        for (TypedAccountConfig accountConfig : accountConfigs) {
            String type = accountConfig.getType();

            List<AccountBaseInfo> accountBaseInfos = loadBaseAccountInfoFromFile(type, accountConfig.getAccountFileUserDirPath());
            // Step 1 初始化账号

            // 加载账户配置文件中的主账户
            List<AccountContext> mainAccounts = buildAccountContext(accountConfig.getProxyType(), accountBaseInfos);

            typedAccountContextMap.put(type, mainAccounts);
        }

        return typedAccountContextMap;
    }


    /**
     * 构建depinClientAccounts
     *
     * @param accountBaseInfos depinClientAccounts
     * @return AccountContext
     */
    private List<AccountContext> buildAccountContext(ProxyType proxyType, List<AccountBaseInfo> accountBaseInfos) {
        List<AccountContext> newAccountContexts = new ArrayList<>();

        List<AccountContext> noProxyIds = new ArrayList<>();
        List<AccountContext> noBrowserEnvIds = new ArrayList<>();

        AbstractProxyPool proxyPool = switch (proxyType) {
            case STATIC -> getStaticProxyPool();
            case DYNAMIC -> getDynamicProxyPool();
            case NO -> null;
        };

        BrowserEnvPool browserEnvPool = getBrowserEnvPool();
        TwitterPool twitterPool = getTwitterPool();

        accountBaseInfos
                .forEach(depinClientAccount -> {
                    AccountContext accountContext = AccountContext.builder()
                            .accountBaseInfo(depinClientAccount)
                            .params(new HashMap<>())
                            .build();

                    Integer id = depinClientAccount.getId();


                    // 代理设置
                    if (proxyPool == null) {
                        log.debug("账户[{}]不使用代理", accountContext.getAccountBaseInfo().getName());
                    } else if (depinClientAccount.getProxyId() != null) {
                        NetworkProxy proxy = proxyPool.getItem(depinClientAccount.getProxyId());
                        accountContext.setProxy(proxy);

                        log.info("账号:{},将使用代理:{}[{}]", accountContext.getName(),
                                proxy.getProxyType(), proxy.getAddressStr());
                    } else if (id != null && id < proxyPool.size()) {
                        accountContext.getAccountBaseInfo().setProxyId(id);
                        NetworkProxy proxy = proxyPool.getItem(id);

                        accountContext.setProxy(proxy);
                        log.info("账号:{},将使用代理:{}[{}]", accountContext.getName(),
                                proxy.getProxyType(), proxy.getAddressStr());
                    } else {
                        noProxyIds.add(accountContext);
                    }

                    // 账号没有配置浏览器环境
                    if (depinClientAccount.getBrowserEnvId() != null) {
                        accountContext.setBrowserEnv(browserEnvPool.getItem(depinClientAccount.getBrowserEnvId()));
                    } else if (id != null && id < browserEnvPool.size()) {
                        accountContext.getAccountBaseInfo().setBrowserEnvId(id);
                        accountContext.setBrowserEnv(browserEnvPool.getItem(id));
                    } else {
                        noBrowserEnvIds.add(accountContext);
                    }

                    // 添加推特
                    if (depinClientAccount.getTwitterId() != null) {
                        accountContext.setTwitter(twitterPool.getItem(depinClientAccount.getTwitterId()));
                    } else if (id != null && id < twitterPool.size()) {
                        accountContext.getAccountBaseInfo().setTwitterId(id);
                        accountContext.setTwitter(twitterPool.getItem(id));
                    }

                    newAccountContexts.add(accountContext);
                });

        // Step 2 账号没代理的尝试给他设置代理
        if (!noProxyIds.isEmpty() && proxyPool != null) {
            log.warn("以下账号没有配置代理，将随机选择一个代理进行使用");

            // 静态代理，给账户分配使用，用完不重复分配
            List<NetworkProxy> lessUsedItem = proxyPool.getLessUsedItem(noProxyIds.size());

            for (int i = 0; i < noProxyIds.size(); i++) {
                AccountContext accountContext = noProxyIds.get(i);

                NetworkProxy proxy = lessUsedItem.get(i);
                accountContext.setProxy(proxy);

                log.info("账号:{},将使用代理:{}[{}]", accountContext.getName(), proxy.getProxyType(), proxy.getAddressStr());
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
     * 从文件加载账户基础信息
     *
     * @param type                   type
     * @param accountFileUserDirPath accountFileUserDirPath
     * @return List<AccountBaseInfo>
     */
    private List<AccountBaseInfo> loadBaseAccountInfoFromFile(String type, String accountFileUserDirPath) {
        String resourcePath = FileUtil.getConfigDirResourcePath(List.of(), accountFileUserDirPath);

        try (BufferedReader reader = new BufferedReader(new FileReader(resourcePath))) {
            List<AccountBaseInfo> accountBaseInfos = new ArrayList<>();

            String line;
            int id = 0;
            while ((line = reader.readLine()) != null) {
                AccountBaseInfo baseInfo = new AccountBaseInfo(line);
                baseInfo.setId(id++);
                baseInfo.setType(type);
                accountBaseInfos.add(baseInfo);
            }

            return accountBaseInfos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
