
package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTypeHandler;
import com.baomidou.mybatisplus.annotation.*;


        import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("t_reword_info")
@PropertyChangeListenClass
public class RewordInfo {

    @TableId(value = "project_account_id", type = IdType.INPUT)
    private Integer projectAccountId;

    @TableField("total_points")
    private Double totalPoints;

    @TableField("session")
    private String session;

    @TableField("session_points")
    private Double sessionPoints;

    @TableField("daily_points")
    private Double dailyPoints;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;



    public RewordInfo newInstance() {
        RewordInfo rewordInfo = new RewordInfo();
        rewordInfo.totalPoints = this.totalPoints;
        rewordInfo.session = this.session;
        rewordInfo.sessionPoints = this.sessionPoints;
        rewordInfo.dailyPoints = this.dailyPoints;
        rewordInfo.insertDatetime = this.insertDatetime;
        rewordInfo.updateDatetime = this.updateDatetime;
        rewordInfo.isValid = this.isValid;

        return rewordInfo;
    }
}
