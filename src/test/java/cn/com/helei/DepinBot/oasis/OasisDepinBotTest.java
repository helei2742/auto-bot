package cn.com.helei.DepinBot.oasis;

import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OasisDepinBotTest {

    private static OasisDepinBot oasisDepinBot;
    @BeforeAll
    static void setUp() throws DepinBotStartException {
        oasisDepinBot = new OasisDepinBot("app/oasis.yaml");
        oasisDepinBot.init();
    }


    @Test
    public void testBot() throws DepinBotStartException {
        oasisDepinBot.start();

    }
}
