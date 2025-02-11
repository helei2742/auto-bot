package cn.com.helei.bot.view;

import cn.com.helei.bot.core.AutoBotApplication;
import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.supporter.commandMenu.MenuNodeMethod;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;
import cn.com.helei.bot.core.supporter.netty.BotJsonWSClient;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import cn.com.helei.bot.core.util.exception.DepinBotStartException;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest(classes = AutoBotApplication.class)
class MenuCMDLineAutoBotTest {

    private static final Logger log = LoggerFactory.getLogger(MenuCMDLineAutoBotTest.class);

    private TestRestAutoBot autoBot;

    private MenuCMDLineAutoBot<AutoBotConfig> menuCMDLineAutoBot;

    @Autowired
    public BotApi botApi;

    @BeforeEach
    public void setUp() throws DepinBotStartException {
        AutoBotConfig load = YamlConfigLoadUtil.load("config.app", "example.yaml", "bot.app", AutoBotConfig.class);

        autoBot = new TestRestAutoBot(load, botApi);

        menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(autoBot, List.of(DefaultMenuType.IMPORT));

    }

    @Test
    public void test() throws DepinBotStartException {
        menuCMDLineAutoBot.start();
    }


    @BotApplication(name = "test", describe = "dwadwa")
    static class TestRestAutoBot extends AnnoDriveAutoBot<TestRestAutoBot> {

        public TestRestAutoBot(AutoBotConfig autoBotConfig, BotApi botApi) {
            super(autoBotConfig, botApi);
        }

        @Override
        protected TestRestAutoBot getInstance() {
            return this;
        }

        @BotMethod(jobType = BotJobType.REGISTER)
        public Result register(AccountContext accountContext, String inviteCode) {
            return Result.ok("success");
        }

        @BotMethod(jobType = BotJobType.LOGIN)
        public Result login(AccountContext accountContext) {
            return Result.ok();
        }

        @BotMethod(jobType = BotJobType.TIMED_TASK, cronExpression = "*/10 * * * * ?")
        public String dailyCheckIn(AccountContext accountContext) {
            System.out.println("123123");
            log.info("签到逻辑");
            return "签到成功";
        }

        @BotMethod(
                jobType = BotJobType.WEB_SOCKET_CONNECT,
                jobName = "ws-keep-alive-task",
                cronExpression = "*/10 * * * * ?"
        )
        public BaseBotWSClient<?, ?> keepAliveTask(AccountContext accountContext) {
            log.info("[{}]创建ws客户端", accountContext.getId());

            return new BotJsonWSClient(accountContext, "http://127.0.0.1:443/test") {
                @Override
                public JSONObject getHeartbeatMessage() {
                    return null;
                }

                @Override
                public void whenAccountReceiveResponse(Object id, JSONObject response) {

                }

                @Override
                public void whenAccountReceiveMessage(JSONObject message) {

                }
            };
        }

        @MenuNodeMethod(title = "测试", description = "账户1，param -> 随机数")
        public String test() {
            getAccountContexts().getFirst().setParam("test", String.valueOf(RandomUtil.randomInt()));
            return "";
        }
    }
}
