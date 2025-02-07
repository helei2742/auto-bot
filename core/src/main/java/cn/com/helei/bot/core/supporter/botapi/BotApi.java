package cn.com.helei.bot.core.supporter.botapi;


import cn.com.helei.bot.core.mvc.service.*;

public interface BotApi {

    IProjectInfoService getProjectInfoService();

    IBotInfoService getBotInfoService();

    IAccountBaseInfoService getAccountBaseInfoService();

    IBrowserEnvService getBrowserEnvService();

    IDiscordAccountService getDiscordAccountService();

    IBotAccountContextService getBotAccountContextService();

    IProxyInfoService getProxyInfoService();

    IRewordInfoService getRewordInfoService();

    ITwitterAccountService getTwitterAccountService();

    ImportService getImportService();
}
