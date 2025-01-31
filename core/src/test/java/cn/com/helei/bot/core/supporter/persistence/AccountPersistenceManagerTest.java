package cn.com.helei.bot.core.supporter.persistence;

import cn.com.helei.bot.core.dto.RewordInfo;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.dto.account.AccountBaseInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class AccountPersistenceManagerTest {
    @Test
    void bindPersistenceAnnoListener() {
        AccountPersistenceManager accountPersistenceManager = new AccountPersistenceManager("test");

        AccountBaseInfo clientAccount = new AccountBaseInfo();
        AccountContext accountContext = AccountContext.builder().accountBaseInfo(clientAccount).rewordInfo(new RewordInfo()).build();

        accountContext.setParams(new HashMap<>());
        AccountContext proxy = accountPersistenceManager.bindPersistenceAnnoListener(accountContext, "");

        accountContext.setParam("2", "2");
//        proxy.setParam("1", "a");
//        proxy.setParam("1", "b");
//        proxy.setParam("1", "c");
//        proxy.setParam("1", "d");
//
        proxy.getRewordInfo().setTotalPoints(1.0);
//        accountContext.setUsable(false);
//        accountContext.getClientAccount().setName("123");

        System.out.println(proxy.getRewordInfo().getTotalPoints());
    }
}
