package cn.com.helei.DepinBot.core.pool.network;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
        import java.util.ArrayList;
import java.util.List;

public class TxtProxyConvert2YamlConfig {

    public static void main(String[] args) throws RuntimeException {

        String fileName = "/Users/helei/develop/ideaworkspace/depinbot/DepinBot/src/main/resources/proxy.txt";
        String fileName1 = "/Users/helei/develop/ideaworkspace/depinbot/DepinBot/src/main/resources/build-proxy.yaml";

        try (BufferedReader fr = new BufferedReader(new FileReader(fileName))){

            List<NetworkProxy> set = new ArrayList<>();
            String line;
            int id = 1;
            while ((line = fr.readLine()) != null) {
                String[] split = line.split(":");

                NetworkProxy networkProxy = new NetworkProxy();
                networkProxy.setHost(split[0]);
                networkProxy.setPort(Integer.parseInt(split[1]));
                networkProxy.setUsername(split[2]);
                networkProxy.setPassword(split[3]);
                set.add(networkProxy);
            }

            // 配置 YAML 输出选项
            DumperOptions options = new DumperOptions();
            options.setIndent(2);  // 设置缩进级别为 2
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 使用块风格

            Yaml yaml = new Yaml(options);

            // 写入 YAML 文件
            try (FileWriter writer = new FileWriter(fileName1)) {
                NetworkProxyPool data = new NetworkProxyPool();
                data.setList(set);
                yaml.dump(data, writer);  // 将对象写入 YAML 文件
                System.out.println("YAML 文件写入成功！");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
