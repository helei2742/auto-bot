package cn.com.helei.bot.core.util;


import cn.com.helei.bot.core.config.SystemConfig;

import java.io.*;
        import java.nio.file.Path;
import java.util.List;

public class FileUtil {


    public static final String RESOURCE_ROOT_DIR = System.getProperty("user.dir") + File.separator + "botData";

    public static String getConfigDirResourcePath(List<String> path, String fileName) {
        StringBuilder sb = new StringBuilder(RESOURCE_ROOT_DIR);

        for (String p : path) {
            sb.append(File.separator).append(p);
        }
        return sb.append(File.separator).append(fileName).toString();
    }

    public static String getBotAppConfigPath() {
        return RESOURCE_ROOT_DIR +File.separator + String.join(File.separator, SystemConfig.CONFIG_DIR_APP_PATH);
    }

    /**
     * 保存账户
     */
    public static void saveJSONStringContext(Path filePath, String jsonContext) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(jsonContext);
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println(getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, "account.yaml"));
    }
}
