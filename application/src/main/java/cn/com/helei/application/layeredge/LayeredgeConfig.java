package cn.com.helei.application.layeredge;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class LayeredgeConfig extends cn.com.helei.bot.core.BaseDepinBotConfig {


    private List<String> publicKeys;

    public static LayeredgeConfig loadYamlConfig(String classpath) {
        log.info("开始加载 layeredge配置信息-file classpath:[{}}", classpath);
        LayeredgeConfig layeredgeConfig = cn.com.helei.bot.core.util.YamlConfigLoadUtil
                .load(cn.com.helei.bot.core.config.SystemConfig.CONFIG_DIR_APP_PATH, classpath, List.of("depin", "app", "layeredge"), LayeredgeConfig.class);

        log.info("layeredge配置信息加载完毕,{}", layeredgeConfig);
        return layeredgeConfig;
    }
}
