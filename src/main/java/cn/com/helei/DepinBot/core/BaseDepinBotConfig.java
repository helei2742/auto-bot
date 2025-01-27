package cn.com.helei.DepinBot.core;


import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class BaseDepinBotConfig {

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
    private int autoClaimIntervalSeconds = 10;

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
    private String networkPoolConfig = "network-proxy.yaml";

    /**
     * 浏览器环境池配置文件名
     */
    private String browserEnvPoolConfig = "browser-env.yaml";

    /**
     * 账户配置文件名
     */
    private String accountPoolConfig = "account.yaml";


    private Map<String, String> configMap = new HashMap<>();

    public String getConfig(String key) {
        return configMap.get(key);
    }

    public void setConfig(String key, String value) {
        this.configMap.put(key, value);
    }
}
