package cn.com.helei.bot.core.supporter.persistence;

import cn.com.helei.bot.core.dto.RewordInfo;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.pool.account.DepinClientAccount;
import cn.com.helei.bot.core.supporter.persistence.AccountPersistenceManager;
import org.junit.jupiter.api.Test;

class AccountPersistenceManagerTest {
    @Test
    void bindPersistenceAnnoListener() {
        AccountPersistenceManager accountPersistenceManager = new AccountPersistenceManager("test");

        DepinClientAccount clientAccount = new DepinClientAccount();

        AccountContext accountContext = AccountContext.builder().clientAccount(clientAccount).rewordInfo(new RewordInfo()).build();

        AccountContext proxy = accountPersistenceManager.bindPersistenceAnnoListener(accountContext, "");

//        proxy.getParams().put("1", "a");
//        proxy.getParams().put("1", "b");
//        proxy.getParams().put("1", "c");
//        proxy.getParams().put("1", "d");

        proxy.getRewordInfo().setTotalPoints(1.0);
//        accountContext.setUsable(false);
//        accountContext.getClientAccount().setName("123");

        System.out.println(proxy.getRewordInfo().getTotalPoints());
    }
}
