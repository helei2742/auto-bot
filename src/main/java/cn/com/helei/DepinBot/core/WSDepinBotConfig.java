package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.config.SystemConfig;
import cn.com.helei.DepinBot.core.util.YamlConfigLoadUtil;
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
     * websocket 并发数量
     */
    private int wsConnectCount = 5;

    /**
     * 重连接慈湖减少的间隔
     */
    private int reconnectCountDownSecond = 180;


    public static <T> T loadYamlConfig(
            String path,
            String name,
            Class<T> tClass
    ) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, name, List.of(path.split("\\.")), tClass);
    }
}
