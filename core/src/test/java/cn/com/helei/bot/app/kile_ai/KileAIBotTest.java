package cn.com.helei.bot.app.kile_ai;

import cn.com.helei.bot.core.AutoBotApplication;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.dto.config.AutoBotAccountConfig;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.util.exception.DepinBotStartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AutoBotApplication.class)
class KileAIBotTest {

    private MenuCMDLineAutoBot<AutoBotConfig> menuCMDLineAutoBot;

    @Autowired
    public BotApi botApi;

    @BeforeEach
    public void setUp() throws DepinBotStartException {
        AutoBotConfig autoBotConfig = new AutoBotConfig();
        autoBotConfig.setBotKey("kile_ai_main");
        autoBotConfig.setAccountConfig(new AutoBotAccountConfig("kile_ai/kile_ai_google.xlsx"));

        KileAIBot kileAIBot = new KileAIBot(autoBotConfig, botApi);

        menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(kileAIBot, List.of(DefaultMenuType.IMPORT));
    }

    @Test
    public void test() throws DepinBotStartException {
        menuCMDLineAutoBot.start();
    }
}
