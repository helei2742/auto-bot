package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTYpeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@TableName("t_account_base_info")
@AllArgsConstructor
@NoArgsConstructor
public class AccountBaseInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("type")
    private String type;

    @TableField("name")
    private String name;

    @TableField("email")
    private String email;

    @TableField("password")
    private String password;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime updateDatetime;

    @TableField("is_valid")
    private Integer isValid;



    public AccountBaseInfo(Object originLine) {
        String emailAndPassword = (String) originLine;

        String[] split = emailAndPassword.split(", ");
        email = split[0];

        password = split[1];

        if (split.length == 3) {
            name = split[2];
        }
        if (split.length == 4) {
            type = split[3];
        }
    }
}
