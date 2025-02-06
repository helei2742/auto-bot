package cn.com.helei.bot.core.supporter.botapi;


import cn.com.helei.bot.core.mvc.service.*;

public interface BotApi {

    IAccountBaseInfoService getAccountBaseInfoService();

    IBrowserEnvService getBrowserEnvService();

    IDiscordAccountService getDiscordAccountService();

    IProjectAccountContextService getProjectAccountContextService();

    IProxyInfoService getProxyInfoService();

    IRewordInfoService getRewordInfoService();

    ITwitterAccountService getTwitterAccountService();

    ImportService getImportService();
}
