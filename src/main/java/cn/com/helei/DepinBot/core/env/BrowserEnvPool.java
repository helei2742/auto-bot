package cn.com.helei.DepinBot.core.env;


import com.jakewharton.fliptables.FlipTable;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BrowserEnvPool {

    private static final String[] BROWSER_ENV_LIST_PRINT_TABLE_HEADER = new String[]{"ID", "Headers"};

    private static final Map<String, BrowserEnvPool> LOADED_POOL_MAP = new ConcurrentHashMap<>();

    @Getter
    private String configClassPath;

    @Setter
    private Set<BrowserEnv> envs;

    private ConcurrentMap<Integer, BrowserEnv> idMapEnv;

    public static BrowserEnvPool loadYamlBrowserEnvPool(String classpath) {
        return LOADED_POOL_MAP.compute(classpath, (k, pool) -> {
            if (pool == null) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = BrowserEnvPool.class.getClassLoader().getResourceAsStream(classpath)) {
                    Map<String, Object> yamlData = yaml.load(inputStream);
                    Map<String, Object> depin = (Map<String, Object>) yamlData.get("depin");
                    Map<String, Object> browser = (Map<String, Object>) depin.get("browser");

                    BrowserEnvPool envPool = yaml.loadAs(yaml.dump(browser), BrowserEnvPool.class);
                    envPool.idMapEnv = new ConcurrentHashMap<>();
                    envPool.envs.forEach(browserEnv -> envPool.idMapEnv.put(browserEnv.getId(), browserEnv));
                    envPool.configClassPath = classpath;

                    pool = envPool;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return pool;
        });
    }

    public BrowserEnv getBrowserEnv(Integer id) {
        return idMapEnv.get(id);
    }

    public String printPool() {
        String[][] table = new String[envs.size()][BROWSER_ENV_LIST_PRINT_TABLE_HEADER.length];

        List<BrowserEnv> list = envs.stream().toList();
        for (int i = 0; i < list.size(); i++) {
            BrowserEnv env = list.get(i);
            table[i] = new String[]{String.valueOf(env.getId()), env.getHeaders().toString()};
        }

        return FlipTable.of(BROWSER_ENV_LIST_PRINT_TABLE_HEADER, table);
    }
}
