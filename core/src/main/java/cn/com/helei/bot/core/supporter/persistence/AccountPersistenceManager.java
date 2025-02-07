package cn.com.helei.bot.core.supporter.persistence;

import cn.com.helei.bot.core.entity.AccountContext;

import java.util.List;
import java.util.Map;

public interface AccountPersistenceManager {

    void init();

    void persistenceAccountContexts(Map<String, List<AccountContext>> typedAccountMap);

    Map<String, List<AccountContext>> loadAccountContexts(Integer botId);

    void registerPersistenceListener(String type, List<AccountContext> targetList);

}
