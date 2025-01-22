package cn.com.helei.DepinBot.core.util;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;

public class FileUtil {

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
}
