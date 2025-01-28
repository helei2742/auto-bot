package cn.com.helei.bot.core;

import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class WSDepinBotConfig extends BaseDepinBotConfig {

    /**
     * ws是否无限重试
     */
    private boolean wsUnlimitedRetry = false;

    /**
     * 连接url
     */
    private String wsBaseUrl;

    /**
     * 心跳间隔
     */
    private int heartBeatIntervalSecond = 30;

    /**
     * websocket 并发数量
     */
    private int wsConnectCount = 5;

    /**
     * 重连减少的间隔
     */
    private int reconnectCountDownSecond = 180;


    public static <T> T loadYamlConfig(
            String prefix,
            String name,
            Class<T> tClass
    ) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, name, List.of(prefix.split("\\.")), tClass);
    }
}
