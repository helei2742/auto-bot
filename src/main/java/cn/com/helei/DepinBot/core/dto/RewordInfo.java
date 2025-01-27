package cn.com.helei.DepinBot.core.dto;

import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenField;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@PropertyChangeListenClass
public class RewordInfo {

    /**
     * 所有分数
     */
    @PropertyChangeListenField
    private Double totalPoints = 0.0;

    @PropertyChangeListenField
    private String sessionId;

    @PropertyChangeListenField
    private Double sessionPoints = 0.0;

    /**
     * 今天的分数
     */
    @PropertyChangeListenField
    private Double todayPoints = 0.0;

    @PropertyChangeListenField
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
