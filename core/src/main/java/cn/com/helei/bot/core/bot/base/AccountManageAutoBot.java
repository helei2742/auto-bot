package cn.com.helei.bot.core.bot.base;

import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.dto.ACListOptResult;
import cn.com.helei.bot.core.dto.BotACJobResult;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.persistence.impl.DBAccountPersistenceManager;
import cn.com.helei.bot.core.util.exception.DepinBotInitException;
import cn.com.helei.bot.core.supporter.persistence.AccountPersistenceManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;


@Slf4j
public abstract class AccountManageAutoBot extends AbstractAutoBot {

    @Getter
    private final List<AccountContext> accountContexts = new ArrayList<>();

    /**
     * 持久化管理器
     */
    private final AccountPersistenceManager persistenceManager;


    public AccountManageAutoBot(AutoBotConfig autoBotConfig, BotApi botApi) {
        super(autoBotConfig, botApi);

        this.persistenceManager = new DBAccountPersistenceManager(botApi);
    }


    @Override
    protected void doInit() throws DepinBotInitException {
        // Step 1 初始化保存的线程
        persistenceManager.init();

        // Step 2 初始化账户
        initAccounts();
    }

    /**
     * 注册账户
     *
     * @return CompletableFuture<Result>
     */
    public abstract CompletableFuture<ACListOptResult> registerAccount();

    /**
     * 登录并获取token
     *
     * @return CompletableFuture<Result>
     */
    public abstract CompletableFuture<ACListOptResult> loginAndTakeTokenAccount();


    /**
     * 更新账户奖励信息
     *
     * @return CompletableFuture<Result>
     */
    public abstract CompletableFuture<ACListOptResult> updateAccountRewordInfo();


    /**
     * 获取jb name列表
     *
     * @return List<String>
     */
    public abstract Set<String> getBotJobNameList();

    /**
     * 运行指定job
     *
     * @param jobName jobName
     * @return CompletableFuture<Result>
     */
    public abstract BotACJobResult startBotJob(String jobName);


    /**
     * 账号被加载后调用
     *
     * @param accountContexts accountContexts
     */
    protected void accountsLoadedHandler(List<AccountContext> accountContexts) {
    }

    /**
     * 初始化账号方法
     */
    private void initAccounts() throws DepinBotInitException {
        Integer botId = getBotInfo().getId();

        String name = getBotInfo().getName();

        try {
            // Step 1 获取持久化的
            List<AccountContext> accountContexts = persistenceManager
                    .loadAccountContexts(botId, getAutoBotConfig().getBotKey());

            // Step 2 没有保存的数据
            if (accountContexts == null || accountContexts.isEmpty()) {
                log.warn("bot[{}]没有账户数据", name);
            } else {
                log.info("bot[{}]使用历史账户数据, 共[{}]", name, accountContexts.size());

                // Step 3 加载到bot (字段修改监听)
                registerAccountsInBot(accountContexts);

                accountsLoadedHandler(accountContexts);

                this.accountContexts.addAll(accountContexts);
            }
        } catch (Exception e) {
            throw new DepinBotInitException("初始化账户发生错误", e);
        }
    }


    /**
     * 将账户加载到bot， 会注册监听，当属性发生改变时自动刷入磁盘
     *
     * @param accountContexts accountContexts
     */
    private void registerAccountsInBot(List<AccountContext> accountContexts) {
        persistenceManager.registerPersistenceListener(accountContexts);
    }
}
