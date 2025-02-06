package cn.com.helei.bot.core.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-02-06
 */
@Getter
@Setter
@Builder
@TableName("t_telegram_account")
public class TelegramAccount {


    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    @TableField("username")
    @ExcelProperty("username")
    private String username;

    @TableField("password")
    @ExcelProperty("password")
    private String password;

    @TableField("phone_prefix")
    @ExcelProperty("phone_prefix")
    private String phonePrefix;

    @TableField("phone")
    @ExcelProperty("phone")
    private String phone;

    @TableField("token")
    @ExcelProperty("token")
    private String token;

    @TableField("params")
    private Map<String, Object> params;

    @TableField("insert_datetime")
    private LocalDateTime insertDatetime;

    @TableField("update_datetime")
    private LocalDateTime updateDatetime;

    @TableField("is_valid")
    private Integer isValid;
}
