package cn.com.helei.DepinBot.core.util;

import cn.com.helei.DepinBot.core.dto.system.CpuInfo;
import cn.com.helei.DepinBot.core.dto.system.GpuInfo;
import cn.com.helei.DepinBot.core.dto.system.MemoryInfo;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxyPool;
import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Data
public class SystemInfo {

    public final static SystemInfo INSTANCE;

    private static final String RANDOM_ID_TYPE1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final List<String> features = List.of("mmx", "sse", "sse2", "sse3", "ssse3", "sse4_1", "sse4_2", "avx");

    private static final List<Integer> processorCount = List.of(4, 8, 16, 32);

    private static final SecureRandom random = new SecureRandom();

    private List<String> cpuModelNames;

    private List<String> renderers;

    private List<String> vendors;

    private List<String> osList;

    static {
        INSTANCE = loadFromYaml("system-info.yaml");
    }

    /**
     * 获取随机id
     *
     * @param length length
     * @return String
     */
    public String getRandomId(int length) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            result.append(RANDOM_ID_TYPE1.charAt(random.nextInt(RANDOM_ID_TYPE1.length())));
        }

        return result.toString();
    }


    /**
     * 随机cpu信息
     *
     * @return CpuInfo
     */
    public CpuInfo generateRandomCpuInfo() {
        Integer processor = processorCount.get(random.nextInt(processorCount.size()));

        List<CpuInfo.CpuProcessor> cpuProcessors = new ArrayList<>();
        for (int i = 0; i < processor; i++) {
            CpuInfo.CpuProcessor e = new CpuInfo.CpuProcessor();
            e.setUsage(
                    Math.floor(Math.random() * 2000000000000L),
                    Math.floor(Math.random() * 10000000000L),
                    Math.floor(Math.random() * 2000000000000L),
                    Math.floor(Math.random() * 50000000000L)
            );
            cpuProcessors.add(e);
        }

        return CpuInfo.builder()
                .archName("x86_64")
                .features(features)
                .modelName(cpuModelNames.get(random.nextInt(cpuModelNames.size())))
                .numOfProcessors(processor)
                .cpuProcessors(cpuProcessors)
                .temperatures(List.of())
                .build();
    }


    /**
     * 随机GPU信息
     *
     * @return GpuInfo
     */
    public GpuInfo generateRandomGpuInfo() {
        return GpuInfo
                .builder()
                .renderer(renderers.get(random.nextInt(renderers.size())))
                .vendor(vendors.get(random.nextInt(vendors.size())))
                .build();
    }

    /**
     * 随机操作系统信息
     *
     * @return os
     */
    public String generateRandomOperatingSystem() {
        return osList.get(random.nextInt(osList.size()));
    }

    /**
     * 随机内存信息
     *
     * @return JSONObject
     */
    public MemoryInfo generateRandomMemoryInfo() {
        return MemoryInfo
                .builder()
                .availableCapacity((long) (Math.floor(Math.random() * 1000000000) + 1000000000))
                .capacity((long) (Math.floor(Math.random() * 1000000000) + 2000000000))
                .build();
    }

    public static SystemInfo loadFromYaml(String classpath) {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = NetworkProxyPool.class.getClassLoader().getResourceAsStream(classpath)) {
            Map<String, Object> yamlData = yaml.load(inputStream);
            Map<String, Object> bot = (Map<String, Object>) yamlData.get("config/bot");
            Map<String, Object> system = (Map<String, Object>) bot.get("system");

            return yaml.loadAs(yaml.dump(system), SystemInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("价值配置网络代理池文件[%s]发生错误", classpath));
        }
    }


    public static void main(String[] args) {
        CpuInfo x = INSTANCE.generateRandomCpuInfo();
        System.out.println(x);
        System.out.println(INSTANCE.generateRandomGpuInfo());
    }
}
