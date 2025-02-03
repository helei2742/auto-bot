package cn.com.helei.application.unich;

import cn.com.helei.bot.core.config.BaseAutoBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UnichConfig extends BaseAutoBotConfig {

    private String captchaId;

    private String captcha2ApiKey;

    private String captchaUrl;

    public static UnichConfig loadYamlConfig(List<String> path, String fileName) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, fileName, path, UnichConfig.class);
    }
}
