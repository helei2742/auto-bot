package cn.com.helei.application.keitokun;

import cn.com.helei.bot.core.config.WSAutoBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class KeitokunConfig extends WSAutoBotConfig {

    private List<String> extraAccounts;

    private String tokenAndUidFileName;

    public static KeitokunConfig loadYamlConfig(String appDirPath) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, appDirPath, List.of("depin", "app", "keitokun"), KeitokunConfig.class);
    }


    public static void main(String[] args) {
        System.out.println(loadYamlConfig("keitokun.yaml"));
    }
}
