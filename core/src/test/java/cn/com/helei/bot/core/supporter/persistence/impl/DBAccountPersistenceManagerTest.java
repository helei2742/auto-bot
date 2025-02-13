package cn.com.helei.bot.core.supporter.persistence.impl;

import cn.com.helei.bot.core.AutoBotApplication;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.botapi.impl.DBBotApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AutoBotApplication.class)
class DBAccountPersistenceManagerTest {

    @Autowired
    private BotApi botApi;

    DBAccountPersistenceManager dBAccountPersistenceManager;

    @BeforeEach
    void setUp() {
        dBAccountPersistenceManager = new DBAccountPersistenceManager(botApi);
    }

    @Test
    void propertyChangeHandler() {

        AccountContext accountContext = new AccountContext();

        AccountContext ac = dBAccountPersistenceManager.bindPersistenceAnnoListener(accountContext);

        accountContext.setParam("qwe", "111");

        ac.setStatus(1);
        AccountContext.signUpSuccess(ac);

        AccountContext.signUpSuccess(accountContext);
    }
}
