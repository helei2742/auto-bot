package cn.com.helei.bot.core.dto.config;


import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class AutoBotConfig {

    /**
     * 标识bot，不同于bot id， botKey是由用户定义的
     */
    private String botKey;

    /**
     * 项目信息
     */
    private String botClassName;


    /**
     * 配置文件配置
     */
    private AutoBotConfigFilePathConfig filePathConfig = new AutoBotConfigFilePathConfig();

    /**
     * 账户配置
     */
    private AutoBotAccountConfig accountConfig = new AutoBotAccountConfig();

    /**
     * 自定义配置
     */
    private Map<String, Object> customConfig = new HashMap<>();


    public String getConfig(String key) {
        return String.valueOf(customConfig.get(key));
    }

    public void setConfig(String key, String value) {
        this.customConfig.put(key, value);
    }

}
