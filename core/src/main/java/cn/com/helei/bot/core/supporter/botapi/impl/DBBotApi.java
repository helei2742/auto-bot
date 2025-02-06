package cn.com.helei.bot.core.supporter.botapi.impl;

import cn.com.helei.bot.core.mvc.service.*;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DBBotApi implements BotApi {

    @Autowired
    private IAccountBaseInfoService accountBaseInfoService;

    @Autowired
    private ITwitterAccountService twitterAccountService;

    @Autowired
    private IProxyInfoService proxyInfoService;

    @Autowired
    private IBrowserEnvService browserEnvService;

    @Autowired
    private IDiscordAccountService discordAccountService;

    @Autowired
    private IProjectAccountContextService projectAccountContextService;

    @Autowired
    private IRewordInfoService rewordInfoService;

    @Autowired
    private DBImportService importService;
}
