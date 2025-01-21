package cn.com.helei.DepinBot.core.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YamlConfigLoadUtil {

    private static final ConcurrentHashMap<String, Object> LOADED_CONFIG_MAP = new ConcurrentHashMap<>();

    public static  <T> T load(String classpath,
                      List<String> prefixList,
                      Class<T> clazz) {

        Object compute = LOADED_CONFIG_MAP.compute(classpath, (k, config) -> {
            if (config == null) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = YamlConfigLoadUtil.class.getClassLoader().getResourceAsStream(classpath)) {
                    Map<String, Object> yamlData = yaml.load(inputStream);

                    if (prefixList != null) {
                        for (String prefix : prefixList) {
                            yamlData = (Map<String, Object>) yamlData.get(prefix);
                        }
                    }

                    config = yaml.loadAs(yaml.dump(yamlData), clazz);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("价值配置网络代理池文件[%s]发生错误", classpath));
                }
            }

            return config;
        });

        return (T) compute;
    }
}
