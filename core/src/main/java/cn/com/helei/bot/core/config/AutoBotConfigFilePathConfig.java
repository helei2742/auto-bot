package cn.com.helei.bot.core.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AutoBotConfigFilePathConfig {

    /**
     * 网络代理池配置文件名
     */
    private String proxyFileBotConfigPath = "proxy.xlsx";

    /**
     * 浏览器环境池配置文件名
     */
    private String browserEnvFileBotConfigPath = "browser_env.xlsx";

    /**
     * 推特配置文件
     */
    private String twitterFileBotConfigPath = "twitter_account.xlsx";

    /**
     * discord配置文件
     */
    private String discordFileBotConfigPath = "discord_account.xlsx";

    /**
     * tg配置文件
     */
    private String telegramFileBotConfigPath = "telegram_account.xlsx";

    /**
     * 基础账户配置文件
     */
    private String baseAccountFileBotConfigPath = "base_account.xlsx";

}
