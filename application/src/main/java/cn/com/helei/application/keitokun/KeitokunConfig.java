package cn.com.helei.application.keitokun;

import cn.com.helei.bot.core.config.WSDepinBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class KeitokunConfig extends WSDepinBotConfig {

    private List<String> uids;

    private List<String> tokens;

    private List<String> extraAccounts;

    public static KeitokunConfig loadYamlConfig(String classpath) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, classpath, List.of("depin", "app", "keitokun"), KeitokunConfig.class);
    }


    public static void main(String[] args) {
        System.out.println(loadYamlConfig("keitokun.yaml"));
    }
}
