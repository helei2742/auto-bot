package cn.com.helei.DepinBot.core.network;

import cn.com.helei.DepinBot.core.util.table.CommandLineTablePrintHelper;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
        import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkProxyPool {

    private static final Map<String, NetworkProxyPool> LOADED_POOL_MAP = new ConcurrentHashMap<>();

    @Getter
    private String configClassPath;

    @Setter
    @Getter
    private List<NetworkProxy> pool;

    private final ConcurrentMap<Integer, NetworkProxy> idMapProxy = new ConcurrentHashMap<>();

    /**
     * 使用次数
     */
    private final Map<Integer, Integer> useCount = new HashMap<>();


    public static void main(String[] args) {
        System.out.println(loadYamlNetworkPool("network-proxy.yaml"));
    }

    public static NetworkProxyPool loadYamlNetworkPool(String classpath) {
        return LOADED_POOL_MAP.compute(classpath, (k, pool) -> {
            if (pool == null) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = NetworkProxyPool.class.getClassLoader().getResourceAsStream(classpath)) {
                    Map<String, Object> yamlData = yaml.load(inputStream);
                    Map<String, Object> depin = (Map<String, Object>) yamlData.get("depin");
                    Map<String, Object> network = (Map<String, Object>) depin.get("network");
                    Map<String, Object> proxy = (Map<String, Object>) network.get("proxy");

                    NetworkProxyPool networkProxyPool = yaml.loadAs(yaml.dump(proxy), NetworkProxyPool.class);

                    AtomicInteger id = new AtomicInteger();
                    networkProxyPool.pool.forEach(networkProxy -> {
                        networkProxy.setId(id.getAndIncrement());
                        networkProxyPool.idMapProxy.put(id.get(), networkProxy);
                        networkProxyPool.useCount.put(id.get(), 0);
                    });

                    networkProxyPool.configClassPath = classpath;

                    pool = networkProxyPool;
                } catch (IOException e) {
                    throw new RuntimeException(String.format("价值配置网络代理池文件[%s]发生错误", classpath));
                }
            }
            return pool;
        });
    }


    /**
     * 获取最少使用的代理
     *
     * @param count 数量
     * @return List<NetworkProxy>
     */
    public synchronized List<NetworkProxy> getLessUsedProxy(int count) {
        int batchSize = Math.min(count, useCount.size());

        List<NetworkProxy> res = new ArrayList<>(count);

        int needCount = count;
        while (needCount > 0) {
            int currentSize = Math.min(needCount, batchSize);

            List<NetworkProxy> batch = useCount.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                    .limit(currentSize)
                    .map(e -> {
                        useCount.compute(e.getKey(), (k, v) -> v == null ? 0 : v + 1);
                        return idMapProxy.get(e.getKey());
                    }).toList();
            res.addAll(batch);

            needCount -= batch.size();
        }

        return res;
    }

    /**
     * 获取代理
     *
     * @param id id
     * @return NetworkProxy
     */
    public synchronized NetworkProxy getProxy(Integer id) {
        return idMapProxy.compute(id, (k, v) -> {
            if (v == null) return null;

            useCount.compute(id, (k1, v1) -> {
                if (v1 == null) v1 = 0;

                return v1 + 1;
            });

            return v;
        });
    }


    /**
     * 打印池
     *
     * @return String
     */
    public String printPool() {
        return CommandLineTablePrintHelper.generateTableString(pool.stream().toList(), NetworkProxy.class);
    }
}
