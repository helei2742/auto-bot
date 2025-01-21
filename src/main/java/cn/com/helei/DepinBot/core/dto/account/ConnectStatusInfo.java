package cn.com.helei.DepinBot.core.dto.account;

import cn.com.helei.DepinBot.core.constants.ConnectStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Account的连接状态
 */
@Data
public class ConnectStatusInfo {

    /**
     * 开始时间
     */
    private volatile LocalDateTime startDateTime;

    /**
     * 更新时间
     */
    private volatile LocalDateTime updateDateTime;

    /**
     * 心跳数
     */
    private final AtomicInteger heartBeatCount = new AtomicInteger(0);

    /**
     * 错误的心跳数
     */
    private final AtomicInteger errorHeartBeatCount = new AtomicInteger(0);

    /**
     * 连接状态
     */
    private volatile ConnectStatus connectStatus = ConnectStatus.NEW;
}
