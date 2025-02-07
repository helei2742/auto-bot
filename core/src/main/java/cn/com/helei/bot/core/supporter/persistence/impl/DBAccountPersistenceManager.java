package cn.com.helei.bot.core.supporter.persistence.impl;


import cn.com.helei.bot.core.config.TypedAccountConfig;
import cn.com.helei.bot.core.entity.AccountBaseInfo;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.persistence.AbstractPersistenceManager;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeInvocation;
import cn.com.helei.bot.core.util.NamedThreadFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class DBAccountPersistenceManager extends AbstractPersistenceManager {

    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory("database-"));

    private final BotApi botApi;;

    public DBAccountPersistenceManager(BotApi botApi) {
        this.botApi = botApi;
    }

    @Override
    public void init() {

    }

    @Override
    public void persistenceAccountContexts(Map<String, List<AccountContext>> typedAccountMap) {
        for (List<AccountContext> values : typedAccountMap.values()) {
            botApi.getBotAccountContextService().saveBatch(values);
        }
    }

    public Map<String, List<AccountContext>> createAccountContexts(Integer projectId, List<TypedAccountConfig> accountConfigs) {
        if (accountConfigs == null || accountConfigs.isEmpty())
            return new HashMap<>();

        // Step 1 按类型遍历
        return accountConfigs.stream().collect(Collectors.toMap(TypedAccountConfig::getType, typedAccountConfig -> {
            String type = typedAccountConfig.getType();

            // Step 2 根据类型查询对应的AccountBaseInfo
            AccountBaseInfo query = new AccountBaseInfo();
            query.setType(type);

            List<AccountBaseInfo> typeBaseInfos = botApi.getAccountBaseInfoService().list(new QueryWrapper<>(query));

            // Step 3 创建AccountCount
            return typeBaseInfos.stream()
                    .map(accountBaseInfo -> AccountContext.builder().accountBaseInfo(accountBaseInfo).build())
                    .toList();
        }));
    }

    @Override
    public Map<String, List<AccountContext>> loadAccountContexts(Integer botId) {
        // Step 1 加载 projectId 对应的账号
        AccountContext query = new AccountContext();
        query.setBotId(botId);
        query.setParams(null);

        List<AccountContext> accountContexts = botApi
                .getBotAccountContextService().
                list(new QueryWrapper<>(query));

        // Step 2 遍历账号，补充对象
        CompletableFuture<?>[] futures = accountContexts.stream()
                .map(accountContext -> CompletableFuture.runAsync(
                        () -> fillAccountInfo(accountContext), executorService))
                .toArray(CompletableFuture[]::new);

        // Step 3 等待所有任务完成
        for (int i = 0; i < futures.length; i++) {
            try {
                futures[i].get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("{} fill account context info error", i, e);
            }
        }

        // Step 4 按类型分类账号
        return accountContexts.stream().collect(Collectors.groupingBy(AccountContext::getType));
    }

    @Override
    protected void propertyChangeHandler(PropertyChangeInvocation invocation) {
        log.info("对象属性改变了{} {}->{}", invocation.getPropertyName(), invocation.getOldValue(), invocation.getNewValue());
    }


    /**
     * 查询填充账户信息
     *
     * @param accountContext accountContext
     */
    private void fillAccountInfo(AccountContext accountContext) {

        // Step 2.1 绑定基础账号信息
        if (accountContext.getAccountBaseInfoId() != null) {
            accountContext.setAccountBaseInfo(botApi.getAccountBaseInfoService().getById(accountContext.getAccountBaseInfoId()));
        }
        // Step 2,2 绑定推特
        if (accountContext.getTwitterId() != null) {
            accountContext.setTwitter(botApi.getTwitterAccountService().getById(accountContext.getTwitterId()));
        }
        // Step 2,3 绑定 discord
        if (accountContext.getDiscordId() != null) {
            accountContext.setDiscord(botApi.getDiscordAccountService().getById(accountContext.getDiscordId()));
        }
        // Step 2.4 绑定代理
        if (accountContext.getProxyId() != null) {
            accountContext.setProxy(botApi.getProxyInfoService().getById(accountContext.getProxyId()));
        }
        // Step 2.5 绑定浏览器环境
        if (accountContext.getBrowserEnvId() != null) {
            accountContext.setBrowserEnv(botApi.getBrowserEnvService().getById(accountContext.getBrowserEnvId()));
        }
        // Step 2.6 绑定tg
        if (accountContext.getTelegramId() != null) {

        }
        // Step 2.7 绑定钱包
        if (accountContext.getWalletId() != null) {

        }

        // Step 2.8 绑定奖励信息
        if (accountContext.getRewardId() != null) {
            accountContext.setRewordInfo(botApi.getRewordInfoService().getById(accountContext.getRewardId()));
        }
    }
}
