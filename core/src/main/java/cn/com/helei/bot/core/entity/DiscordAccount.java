package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTypeHandler;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;

        import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
@Builder
@TableName("t_discord_account")
public class DiscordAccount {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("password")
    @ExcelProperty("password")
    private String password;

    @TableField("username")
    @ExcelProperty("username")
    private String username;

    @TableField("bind_email")
    @ExcelProperty("bind_email")
    private String bindEmail;

    @TableField("bind_email_password")
    @ExcelProperty("bind_email_password")
    private String bindEmailPassword;

    @TableField("token")
    @ExcelProperty("token")
    private String token;

    @TableField("params")
    private Map<String, Object> params;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;

}
