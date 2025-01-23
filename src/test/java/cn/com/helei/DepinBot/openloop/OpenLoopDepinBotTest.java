package cn.com.helei.DepinBot.openloop;

import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenLoopDepinBotTest {

    static OpenLoopDepinBot openLoopDepinBot;


    @BeforeAll
    static void beforeAll() {
        openLoopDepinBot = new OpenLoopDepinBot("app/openloop.yaml");
        openLoopDepinBot.init();
    }

    @Test
    public void test() throws DepinBotStartException {
        openLoopDepinBot.start();
    }

}
