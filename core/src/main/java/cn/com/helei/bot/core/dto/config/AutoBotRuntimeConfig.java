package cn.com.helei.bot.core.dto.config;

import lombok.Data;

@Data
public class AutoBotRuntimeConfig {
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

}
