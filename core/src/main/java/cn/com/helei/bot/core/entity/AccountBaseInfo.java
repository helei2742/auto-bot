package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTypeHandler;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
@TableName("t_account_base_info")
@AllArgsConstructor
@NoArgsConstructor
public class AccountBaseInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("type")
    private String type;

    @TableField("name")
    @ExcelProperty("name")
    private String name;

    @TableField("email")
    @ExcelProperty("email")
    private String email;

    @TableField("password")
    @ExcelProperty("password")
    private String password;

    @TableField("params")
    private Map<String, Object> params = new HashMap<>();

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
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
