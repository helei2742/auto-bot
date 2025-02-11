package cn.com.helei.bot.core.util;

import cn.com.helei.bot.core.config.SystemConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YamlConfigLoadUtil {

    private static final ConcurrentHashMap<String, Object> LOADED_CONFIG_MAP = new ConcurrentHashMap<>();

    public static <T> T load(
            String path,
            String fileName,
            String prefix,
            Class<T> clazz
    ) {
        return load(
                Arrays.asList(path.split("\\.")),
                fileName,
                Arrays.asList(prefix.split("\\.")),
                clazz
        );
    }


    public static <T> T load(
            List<String> path,
            String fileName,
            List<String> prefixList,
            Class<T> clazz
    ) {
        String dirResourcePath = FileUtil.getConfigDirResourcePath(path, fileName);

        Object compute = LOADED_CONFIG_MAP.compute(dirResourcePath, (k, config) -> {
            if (config == null) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = new FileInputStream(dirResourcePath)) {
                    Map<String, Object> yamlData = yaml.load(inputStream);

                    if (prefixList != null) {
                        for (String prefix : prefixList) {
                            yamlData = (Map<String, Object>) yamlData.get(prefix);
                        }
                    }

                    config = yaml.loadAs(yaml.dump(yamlData), clazz);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("加载配置池文件[%s]发生错误", dirResourcePath), e);
                }
            }

            return config;
        });

        return (T) compute;
    }

    public static List<Object> load(List<String> configDirBotPath, String fileName, String prefix) {
        return load(configDirBotPath, fileName, List.of(prefix.split("\\.")));
    }

    public static List<Object> load(
            List<String> path,
            String fileName,
            List<String> prefixList
    ) {
        String dirResourcePath = FileUtil.getConfigDirResourcePath(path, fileName);

        Object compute = LOADED_CONFIG_MAP.compute(dirResourcePath, (k, config) -> {
            if (config == null) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = new FileInputStream(dirResourcePath)) {
                    Map<String, Object> yamlData = yaml.load(inputStream);

                    if (prefixList != null) {
                        for (String prefix : prefixList) {
                            yamlData = (Map<String, Object>) yamlData.get(prefix);
                        }
                    }
                    return yamlData.get("list");
                } catch (IOException e) {
                    throw new RuntimeException(String.format("加载配置池文件[%s]发生错误", dirResourcePath), e);
                }
            }

            return config;
        });

        return (List<Object>) compute;
    }


    public static <T> T load(File path, List<String> prefixList, Class<T> tClass) {
        Object compute = LOADED_CONFIG_MAP.compute(path.getAbsolutePath(), (k, config) -> {
            if (config == null) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = new FileInputStream(path)) {
                    Map<String, Object> yamlData = yaml.load(inputStream);

                    if (prefixList != null) {
                        for (String prefix : prefixList) {
                            yamlData = (Map<String, Object>) yamlData.get(prefix);
                        }
                    }

                    return yaml.loadAs(yaml.dump(yamlData), tClass);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("加载配置池文件[%s]发生错误", path), e);
                }
            }
            return config;
        });

        return (T) compute;
    }

    public static void main(String[] args) {
        System.out.println(YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_BOT_PATH, "browser-env.yaml", List.of("bot", "browser")));
    }

}
