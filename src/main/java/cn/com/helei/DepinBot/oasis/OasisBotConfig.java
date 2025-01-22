package cn.com.helei.DepinBot.oasis;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.pool.account.DepinClientAccount;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class OasisBotConfig extends BaseDepinBotConfig {

    private String inviteCode;

    public static void main(String[] args) {
        System.out.println(loadYamlConfig("app/oasis.yaml"));
    }

    public static OasisBotConfig loadYamlConfig(String classpath) {
        Yaml yaml = new Yaml();
        log.info("开始加载 OasisBot配置信息-file classpath:[{}}", classpath);
        try (InputStream inputStream = OasisBotConfig.class.getClassLoader().getResourceAsStream(classpath)) {
            Map<String, Object> yamlData = yaml.load(inputStream);
            Map<String, Object> depin = (Map<String, Object>) yamlData.get("depin");
            Map<String, Object> app = (Map<String, Object>) depin.get("app");
            Map<String, Object> oasis = (Map<String, Object>) app.get("oasis");

            //Step 1 基础配置文件
            OasisBotConfig oasisBotConfig = yaml.loadAs(yaml.dump(oasis), OasisBotConfig.class);

            log.info("OasisBot配置信息加载完毕");

            return oasisBotConfig;
        } catch (IOException e) {
            throw new RuntimeException(String.format("加载配置文件[%s]发生错误", classpath));
        }
    }
}
