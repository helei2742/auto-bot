package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTYpeHandler;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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
    private String userAgent;

    @TableField("other_header_json")
    private JSONObject otherHeaderJson;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime updateDatetime;

    @TableField("is_valid")
    private Integer isValid;

    public Map<String, String> getHeaders() {
        HashMap<String, String> map = new HashMap<>();
        map.put("User-Agent", userAgent);
        otherHeaderJson.forEach((k,v)-> map.put(k, StrUtil.toString(v)));
        return map;
    }
}
