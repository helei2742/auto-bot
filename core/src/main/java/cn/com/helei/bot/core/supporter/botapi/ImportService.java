package cn.com.helei.bot.core.supporter.botapi;

import java.util.Map;

public interface ImportService {

    Integer importBrowserEnvFromExcel(String fileBotConfigPath);


    Integer importProxyFromExcel(String botConfigPath);


    Map<String, Integer> importAccountBaseInfoFromExcel(String botConfigPath);


    Integer importTwitterFromExcel(String fileBotConfigPath);


    Integer importDiscordFromExcel(String fileBotConfigPath);


    Integer importTelegramFormExcel(String fileBotConfigPath);
}
