package cn.com.helei.DepinBot.core.dto;

import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenClass;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@PropertyChangeListenClass
public class RewordInfo {

    /**
     * 所有分数
     */
    private Double totalPoints = 0.0;

    private String sessionId;

    private Double sessionPoints = 0.0;

    /**
     * 今天的分数
     */
    private Double todayPoints = 0.0;

    private LocalDateTime updateTime;

    public void addTotalPoints(Double points) {
        if (points == null) return;
        this.totalPoints += points;
    }

    public void addSessionPoints(Double points) {
        if (points == null) return;
        this.sessionPoints += points;
    }

    public void addTodayPoints(Double points) {
        if (points == null) return;
        this.todayPoints += points;
    }
}
