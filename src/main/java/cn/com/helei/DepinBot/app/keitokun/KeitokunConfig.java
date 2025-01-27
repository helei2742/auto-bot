package cn.com.helei.DepinBot.app.keitokun;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.WSDepinBotConfig;
import cn.com.helei.DepinBot.core.config.SystemConfig;
import cn.com.helei.DepinBot.core.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class KeitokunConfig extends WSDepinBotConfig {

    private List<String> uids;


    public static KeitokunConfig loadYamlConfig(String classpath) {
        return YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH, classpath, List.of("depin", "app", "keitokun"), KeitokunConfig.class);
    }


    public static void main(String[] args) {
        System.out.println(loadYamlConfig("keitokun.yaml"));
    }
}
