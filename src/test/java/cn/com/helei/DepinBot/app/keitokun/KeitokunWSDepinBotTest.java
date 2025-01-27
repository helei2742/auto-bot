package cn.com.helei.DepinBot.app.keitokun;

import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeitokunWSDepinBotTest {

    static KeitokunWSDepinBot mKeitokunWSDepinBot;

    @BeforeAll
    static void setUp() {

        mKeitokunWSDepinBot = new KeitokunWSDepinBot(KeitokunConfig.loadYamlConfig("keitokun.yaml"));

        mKeitokunWSDepinBot.init();
    }

    @Test
    public void test() throws DepinBotStartException {
        mKeitokunWSDepinBot.start();
    }
}
