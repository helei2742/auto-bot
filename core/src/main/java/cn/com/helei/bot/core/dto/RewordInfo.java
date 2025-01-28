package cn.com.helei.bot.core.dto;

import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PropertyChangeListenClass
public class RewordInfo {

    /**
     * 所有分数
     */
    @PropertyChangeListenField
    private volatile Double totalPoints;

    @PropertyChangeListenField
    private volatile String sessionId;

    @PropertyChangeListenField
    private volatile Double sessionPoints;

    /**
     * 今天的分数
     */
    @PropertyChangeListenField
    private volatile Double todayPoints;

    @PropertyChangeListenField
    private volatile LocalDateTime updateTime;

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

    public RewordInfo newInstance() {
        RewordInfo rewordInfo = new RewordInfo();
        rewordInfo.totalPoints = this.totalPoints;
        rewordInfo.sessionId = this.sessionId;
        rewordInfo.sessionPoints = this.sessionPoints;
        rewordInfo.todayPoints = this.todayPoints;
        rewordInfo.updateTime = this.updateTime;

        return rewordInfo;
    }
}
