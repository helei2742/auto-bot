package cn.com.helei.bot.app.nodego;

import cn.com.helei.bot.core.AutoBotApplication;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.constants.MapConfigKey;
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
class NodeGoBotTest {
    private NodeGoBot nodeGoBot;

    private MenuCMDLineAutoBot<AutoBotConfig> menuCMDLineAutoBot;

    @Autowired
    public BotApi botApi;

    @BeforeEach
    public void setUp() throws DepinBotStartException {
        AutoBotConfig autoBotConfig = new AutoBotConfig();
        autoBotConfig.setConfig("2_CAPTCHA_API_KEY", "c19f149898c436b4c32377504fdf3170");
        autoBotConfig.setConfig(MapConfigKey.INVITE_CODE_KEY, "NODEE57370DAFC3B");
        autoBotConfig.setBotKey("NodeGo-test");

        nodeGoBot = new NodeGoBot(autoBotConfig, botApi);

        menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(nodeGoBot, List.of(DefaultMenuType.IMPORT));
    }

    @Test
    void autoRegister() throws DepinBotStartException {
        menuCMDLineAutoBot.start();
    }
}
