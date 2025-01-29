package cn.com.helei.application.oasis;

import cn.com.helei.bot.core.config.WSDepinBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class OasisBotConfig extends WSDepinBotConfig {

    private String inviteCode;

    public static void main(String[] args) {
        System.out.println(loadYamlConfig("app/oasis.yaml"));
    }

    public static OasisBotConfig loadYamlConfig(String classpath) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, classpath, List.of("depin", "app", "oasis"), OasisBotConfig.class);
    }
}
