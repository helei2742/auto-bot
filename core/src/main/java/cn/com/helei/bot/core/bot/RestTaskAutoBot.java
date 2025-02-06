package cn.com.helei.bot.core.bot;


import cn.com.helei.bot.core.bot.base.AccountManageAutoBot;
import cn.com.helei.bot.core.config.AutoBotConfig;
import cn.com.helei.bot.core.supporter.botapi.BotApi;

public abstract class RestTaskAutoBot extends AccountManageAutoBot {

    public RestTaskAutoBot(AutoBotConfig autoBotConfig, BotApi botApi) {
        super(autoBotConfig, botApi);
    }

}
