package cn.com.helei.bot.view;

import cn.com.helei.bot.core.AutoBotApplication;
import cn.com.helei.bot.core.bot.RestTaskAutoBot;
import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.config.AutoBotConfig;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.supporter.commandMenu.MenuNodeMethod;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import cn.com.helei.bot.core.util.exception.DepinBotStartException;
import cn.hutool.core.util.RandomUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.mail.Message;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@SpringBootTest(classes = AutoBotApplication.class)
class MenuCMDLineAutoBotTest {

    private TestRestAutoBot autoBot;

    private MenuCMDLineAutoBot<AutoBotConfig> menuCMDLineAutoBot;

    @Autowired
    public BotApi botApi;

    @BeforeEach
    public  void setUp() throws DepinBotStartException {
        AutoBotConfig load = YamlConfigLoadUtil.load("config.app", "example/example.yaml", "bot.app", AutoBotConfig.class);

        autoBot = new TestRestAutoBot(load, botApi);

        menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(autoBot, List.of(DefaultMenuType.IMPORT));

    }

    @Test
    public void test() throws DepinBotStartException {
        menuCMDLineAutoBot.start();
    }


    @BotApplication(name = "test", describe = "dwadwa")
    static class TestRestAutoBot extends RestTaskAutoBot {

        public TestRestAutoBot(AutoBotConfig autoBotConfig, BotApi botApi) {
            super(autoBotConfig, botApi);
        }

        @Override
        public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
            return null;
        }

        @Override
        public CompletableFuture<Boolean> verifierAccountEmail(AccountContext accountContext, Message message) {
            return null;
        }

        @Override
        public CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
            return null;
        }

        @Override
        public boolean doAccountClaim(AccountContext accountContext) {
            return false;
        }

        @Override
        public CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
            return null;
        }

        @MenuNodeMethod(title = "测试", description = "账户1，param -> 随机数")
        public String test() {
            getAccounts().get(0).setParam("test", String.valueOf(RandomUtil.randomInt()));
            return "";
        }
    }
}
