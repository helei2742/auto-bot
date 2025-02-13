package cn.com.helei.bot.core.supporter.botapi;


import java.util.List;
import java.util.Map;

public interface ImportService {

    Integer importBotAccountContextFromExcel(Integer botId, String botKey, String fileBotConfigPath);

    Integer importBotAccountContextFromRaw(Integer botId, String botKey, List<Map<String, Object>> rawLines);


    Map<String, Integer> importAccountBaseInfoFromExcel(String botConfigPath);

    Integer importAccountBaseInfoFromRaw(List<Map<String, Object>> rawLines);


    Integer importBrowserEnvFromExcel(String fileBotConfigPath);

    Integer importBrowserEnvFromRaw(List<Map<String, Object>> rawLines);


    Integer importProxyFromExcel(String botConfigPath);

    Integer importProxyFromRaw(List<Map<String, Object>> rawLines);



    Integer importTwitterFromExcel(String fileBotConfigPath);

    Integer importTwitterFromRaw(List<Map<String, Object>> rawLines);


    Integer importDiscordFromExcel(String fileBotConfigPath);

    Integer importDiscordFromRaw(List<Map<String, Object>> rawLines);


    Integer importTelegramFormExcel(String fileBotConfigPath);

    Integer importTelegramFormRaw(List<Map<String, Object>> rawLines);
}
