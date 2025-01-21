package cn.com.helei.DepinBot.core;


import cn.com.helei.DepinBot.core.pool.account.DepinClientAccount;
import lombok.Data;

import java.util.List;

@Data
public abstract class BaseDepinBotConfig {

    /**
     * 名字
     */
    private String name = "Default Name";

    /**
     * 并发数量
     */
    private int concurrentCount = 5;


    /**
     * 账户奖励刷新间隔
     */
    private long accountRewardRefreshIntervalSeconds = 600;

    /**
     * 网络代理池配置文件名
     */
    private String networkPoolConfig = "bot/network-proxy.yaml";

    /**
     * 浏览器环境池配置文件名
     */
    private String browserEnvPoolConfig = "bot/browser-env.yaml";

    /**
     * 连接url
     */
    private String wsBaseUrl;


    public abstract List<DepinClientAccount> getAccountList();
}
