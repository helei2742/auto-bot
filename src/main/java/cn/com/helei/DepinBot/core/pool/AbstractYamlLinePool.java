package cn.com.helei.DepinBot.core.pool;


import cn.com.helei.DepinBot.core.util.YamlConfigLoadUtil;
import cn.com.helei.DepinBot.core.util.table.CommandLineTablePrintHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class AbstractYamlLinePool<T extends AbstractYamlLineItem> {

    private final Class<T> tClass;

    @Getter
    @Setter
    private List<T> list;

    @Setter
    private String configClassPath;


    private final ConcurrentMap<Integer, AbstractYamlLineItem> idMapItem = new ConcurrentHashMap<>();

    /**
     * 使用次数
     */
    private final Map<Integer, Integer> useCountMap = new HashMap<>();

    public AbstractYamlLinePool(Class<T> tClass) {
        this.tClass = tClass;
    }

    public static <C extends AbstractYamlLinePool<?>> C loadYamlPool(
            String classpath, String path, Class<C> cClass) {

        C pool = YamlConfigLoadUtil.load(classpath, Arrays.stream(path.split("\\.")).toList(), cClass);

        AtomicInteger id = new AtomicInteger();
        pool.getList().forEach(item -> {

            item.setId(id.getAndIncrement());
            pool.getIdMapItem().put(id.get(), item);
            pool.getUseCountMap().put(id.get(), 0);
        });

        pool.setConfigClassPath(classpath);

        return pool;
    }

    /**
     * 获取代理
     *
     * @param id id
     * @return NetworkProxy
     */
    public synchronized T getItem(Integer id) {
        AbstractYamlLineItem compute = idMapItem.compute(id, (k, v) -> {
            if (v == null) return null;

            useCountMap.compute(id, (k1, v1) -> {
                if (v1 == null) v1 = 0;

                return v1 + 1;
            });

            return v;
        });
        return (T) compute;
    }



    /**
     * 获取最少使用的
     *
     * @param count 数量
     * @return List<T>
     */
    public synchronized List<T> getLessUsedItem(int count) {
        int batchSize = Math.min(count, getUseCountMap().size());

        List<T> res = new ArrayList<>(count);

        int needCount = count;
        while (needCount > 0) {
            int currentSize = Math.min(needCount, batchSize);

            List<T> batch = getUseCountMap().entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                    .limit(currentSize)
                    .map(e -> {
                        getUseCountMap().compute(e.getKey(), (k, v) -> v == null ? 0 : v + 1);
                        return (T) getIdMapItem().get(e.getKey());
                    }).toList();
            res.addAll(batch);

            needCount -= batch.size();
        }

        return res;
    }


    /**
     * 打印池
     *
     * @return String
     */
    public String printPool() {
        return CommandLineTablePrintHelper.generateTableString(list, tClass);
    }
}
