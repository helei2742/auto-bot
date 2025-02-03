package cn.com.helei.application.openloop;

import cn.com.helei.bot.core.config.BaseAutoBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class OpenLoopConfig  extends BaseAutoBotConfig {

    private String inviteCode;

    public static void main(String[] args) {
        System.out.println(loadYamlConfig("openloop.yaml"));
    }

    public static OpenLoopConfig loadYamlConfig(String classpath) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, classpath, List.of("depin", "app", "openloop"), OpenLoopConfig.class);
    }
}
