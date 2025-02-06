package cn.com.helei.bot.core.bot;


import cn.com.helei.bot.core.bot.base.AccountManageAutoBot;
import cn.com.helei.bot.core.config.BaseAutoBotConfig;
import cn.com.helei.bot.core.supporter.botapi.BotApi;

public abstract class RestTaskAutoBot extends AccountManageAutoBot {

    public RestTaskAutoBot(BaseAutoBotConfig baseAutoBotConfig, BotApi botApi) {
        super(baseAutoBotConfig, botApi);
    }

}
