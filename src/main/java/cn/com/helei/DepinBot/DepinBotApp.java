package cn.com.helei.DepinBot;

import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import cn.com.helei.DepinBot.openloop.OpenLoopDepinBot;

public class DepinBotApp {

    public static void main(String[] args) throws DepinBotStartException {
        OpenLoopDepinBot openLoopDepinBot = new OpenLoopDepinBot("openloop.yaml");
        openLoopDepinBot.init();
        openLoopDepinBot.start();
    }
}
