package cn.com.helei.DepinBot.app.layeredge;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.config.SystemConfig;
import cn.com.helei.DepinBot.core.util.YamlConfigLoadUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class LayeredgeConfig extends BaseDepinBotConfig {


    private List<String> publicKeys;

    public static LayeredgeConfig loadYamlConfig(String classpath) {
        log.info("开始加载 layeredge配置信息-file classpath:[{}}", classpath);
        LayeredgeConfig layeredgeConfig = YamlConfigLoadUtil
                .load(SystemConfig.CONFIG_DIR_APP_PATH, classpath, List.of("depin", "app", "layeredge"), LayeredgeConfig.class);

        log.info("layeredge配置信息加载完毕,{}", layeredgeConfig);
        return layeredgeConfig;
    }
}
