package cn.com.helei.bot.core.supporter.botapi;


import java.util.Map;

public interface ImportService {

    Integer importBotAccountContextFromExcel(Integer botId, String fileBotConfigPath);

    Integer importBrowserEnvFromExcel(String fileBotConfigPath);


    Integer importProxyFromExcel(String botConfigPath);


    Map<String, Integer> importAccountBaseInfoFromExcel(String botConfigPath);


    Integer importTwitterFromExcel(String fileBotConfigPath);


    Integer importDiscordFromExcel(String fileBotConfigPath);


    Integer importTelegramFormExcel(String fileBotConfigPath);
}
