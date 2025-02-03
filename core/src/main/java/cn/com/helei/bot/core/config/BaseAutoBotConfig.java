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
    private String staticPoolConfig = "proxy-static.yaml";

    /**
     * 动态代理池配置文件名
     */
    private String dynamicProxyConfig = "proxy-dynamic.yaml";

    /**
     * 浏览器环境池配置文件名
     */
    private String browserEnvPoolConfig = "browser-env.yaml";

    /**
     * 推特配置文件
     */
    private String twitterPoolConfig = "twitter.yaml";


    private List<TypedAccountConfig> accountConfigs;

    private Map<String, String> configMap = new HashMap<>();

    public String getConfig(String key) {
        return configMap.get(key);
    }

    public void setConfig(String key, String value) {
        this.configMap.put(key, value);
    }
}
