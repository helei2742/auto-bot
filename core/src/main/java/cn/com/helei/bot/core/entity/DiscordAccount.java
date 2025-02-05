package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTYpeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

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
@TableName("t_discord_account")
public class DiscordAccount {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("email")
    private String email;

    @TableField("password")
    private String password;

    @TableField("username")
    private String username;

    @TableField("bind_email")
    private String bindEmail;

    @TableField("bind_email_password")
    private String bindEmailPassword;

    @TableField("token")
    private String token;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime updateDatetime;

    @TableField("is_valid")
    private Integer isValid;
}
