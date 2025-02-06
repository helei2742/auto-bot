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
@TableName("t_twitter_account")
public class TwitterAccount {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("username")
    @ExcelProperty("username")
    private String username;

    @TableField("password")
    @ExcelProperty("password")
    private String password;

    @TableField("email")
    @ExcelProperty("email")
    private String email;

    @TableField("email_password")
    @ExcelProperty("email_password")
    private String emailPassword;

    @TableField("token")
    @ExcelProperty("token")
    private String token;

    @TableField("f2a_key")
    @ExcelProperty("f2a_key")
    private String f2aKey;

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
