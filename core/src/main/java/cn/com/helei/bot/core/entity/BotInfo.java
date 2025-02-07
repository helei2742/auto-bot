package cn.com.helei.bot.core.entity;

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
 * @since 2025-02-07
 */
@Getter
@Setter
@TableName("t_bot_info")
public class BotInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("name")
    private String name;

    @TableField("describe")
    private String describe;

    @TableField("limit_project_ids")
    private String limitProjectIds;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;
}
