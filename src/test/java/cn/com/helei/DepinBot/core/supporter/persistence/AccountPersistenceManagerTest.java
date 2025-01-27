package cn.com.helei.DepinBot.core.supporter.persistence;

import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.pool.account.DepinClientAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountPersistenceManagerTest {
    @Test
    void bindPersistenceAnnoListener() {
        AccountPersistenceManager accountPersistenceManager = new AccountPersistenceManager("test");

        DepinClientAccount clientAccount = new DepinClientAccount();

        AccountContext accountContext = AccountContext.builder().clientAccount(clientAccount).build();

        AccountContext proxy = accountPersistenceManager.bindPersistenceAnnoListener(accountContext, "");

//        proxy.getParams().put("1", "a");
        proxy.getParams().put("1", "b");
        proxy.getParams().put("1", "c");
        proxy.getParams().put("1", "d");

        accountContext.getRewordInfo().setTotalPoints(1.0);
        accountContext.setUsable(false);
        accountContext.getClientAccount().setName("123");
    }
}
