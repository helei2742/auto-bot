package cn.com.helei.bot.core.config;


import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString
public class AutoBotConfig {

    /**
     * 项目id
     */
    private Integer projectId;

    /**
     * 名字
     */
    private String name;

    /**
     * 运行时设置
     */
    private AutoBotRuntimeConfig runtime = new AutoBotRuntimeConfig();

    /**
     * websocket 设置
     */
    private AutoBotWSConfig websocket = new AutoBotWSConfig();

    /**
     * 配置文件配置
     */
    private AutoBotConfigFilePathConfig filePathConfig = new AutoBotConfigFilePathConfig();


    private List<TypedAccountConfig> accountConfigs;

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
