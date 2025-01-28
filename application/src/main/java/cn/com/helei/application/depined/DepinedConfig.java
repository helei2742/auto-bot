package cn.com.helei.application.depined;

import cn.com.helei.bot.core.BaseDepinBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class DepinedConfig extends BaseDepinBotConfig {

    private String inviteCode;

    public static void main(String[] args) {
        System.out.println(loadYamlConfig("app/oasis.yaml"));
    }

    public static DepinedConfig loadYamlConfig(String classpath) {
        log.info("开始加载 OasisBot配置信息-file classpath:[{}}", classpath);
        DepinedConfig oasisBotConfig = YamlConfigLoadUtil
                .load(SystemConfig.CONFIG_DIR_APP_PATH, classpath, List.of("depin", "app", "oasis"), DepinedConfig.class);

        log.info("OasisBot配置信息加载完毕,{}", oasisBotConfig);
        return oasisBotConfig;
    }
}
