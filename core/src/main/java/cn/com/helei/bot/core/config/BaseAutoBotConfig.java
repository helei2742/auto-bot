package cn.com.helei.bot.core.config;


import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString
public class BaseAutoBotConfig {

    /**
     * 项目id
     */
    private Integer projectId;

    /**
     * 名字
     */
    private String name;

    /**
     * 并发数量
     */
    private int concurrentCount = 5;

    /**
     * 自动收获间隔
     */
    private int autoClaimIntervalSeconds = 60;

    /**
     * 是否开启账户收益自动刷新
     */
    private Boolean isAccountRewardAutoRefresh = false;

    /**
     * 账户奖励刷新间隔
     */
    private long accountRewardRefreshIntervalSeconds = 600;

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

    private String discordFileBotConfigPath = "discord_account.xlsx";

    private String telegramFileBotConfigPath = "telegram_account.xlsx";

    private String baseAccountFileBotConfigPath = "base_account.xlsx";


    private List<TypedAccountConfig> accountConfigs;

    private Map<String, Object> configMap = new HashMap<>();

    public String getConfig(String key) {
        return String.valueOf(configMap.get(key));
    }

    public void setConfig(String key, String value) {
        this.configMap.put(key, value);
    }
}
