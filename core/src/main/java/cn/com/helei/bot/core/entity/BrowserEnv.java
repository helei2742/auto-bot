package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTypeHandler;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;

        import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
@TableName("t_browser_env")
public class BrowserEnv {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("user_agent")
    @ExcelProperty("user_agent")
    private String userAgent;

    @TableField("other_header")
    @ExcelProperty("other_header")
    private Map<String, Object> otherHeader;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;


    public Map<String, String> getHeaders() {
        HashMap<String, String> map = new HashMap<>();
        map.put("User-Agent", userAgent);
        otherHeader.forEach((k, v)-> map.put(k, StrUtil.toString(v)));
        return map;
    }
}
