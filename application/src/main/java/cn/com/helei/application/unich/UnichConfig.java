package cn.com.helei.application.unich;

import cn.com.helei.bot.core.config.BaseDepinBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UnichConfig extends BaseDepinBotConfig {

    public List<String> tokens;

    public static UnichConfig loadYamlConfig(List<String> path, String fileName) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, fileName, path, UnichConfig.class);
    }
}
