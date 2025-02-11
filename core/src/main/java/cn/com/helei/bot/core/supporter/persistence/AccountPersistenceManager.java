package cn.com.helei.bot.core.supporter.persistence;

import cn.com.helei.bot.core.entity.AccountContext;

import java.util.List;

public interface AccountPersistenceManager {

    void init();

    void persistenceAccountContexts(List<AccountContext> accountContexts);

    List<AccountContext> loadAccountContexts(Integer botId, String botKey);

    void registerPersistenceListener(List<AccountContext> targetList);

}
