package cn.com.helei.DepinBot.app.layeredge;

import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LayeredgeDepinBotTest {

    static LayeredgeDepinBot layeredgeDepinBot;

    @BeforeAll
    static void setUp() {
        layeredgeDepinBot = new LayeredgeDepinBot(
                LayeredgeConfig.loadYamlConfig("Layeredge.yaml")
        );

        layeredgeDepinBot.init();
    }

    @Test
    public void test() throws DepinBotStartException {
        layeredgeDepinBot.start();
    }
}
