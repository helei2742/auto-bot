package cn.com.helei.bot.core.util.pool;

import cn.com.helei.bot.core.util.tableprinter.CommandLineTablePrintHelper;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
        import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class IdMarkPool<T> {

    private final Class<T> tClass;

    @Getter
    @Setter
    private List<Object> list;

    @Setter
    private String configClassPath;


    private final ConcurrentMap<Integer, IdMarkPoolItem<T>> idMapItem = new ConcurrentHashMap<>();

    /**
     * 使用次数
     */
    private final Map<Integer, Integer> useCountMap = new HashMap<>();

    public IdMarkPool(Class<T> tClass) {
        this.tClass = tClass;
    }

    public static <C> IdMarkPool<C> create(List<C> items, Class<C> cClass) {
        IdMarkPool<C> tIdMarkPool = new IdMarkPool<>(cClass);

        AtomicInteger idCounter = new AtomicInteger();

        for (C item : items) {
            int id = idCounter.getAndIncrement();

            tIdMarkPool.getIdMapItem().put(id, new IdMarkPoolItem<>(id, item));
            tIdMarkPool.getUseCountMap().put(id, 0);
        }

        return tIdMarkPool;
    }

    /**
     * 获取代理
     *
     * @param id id
     * @return NetworkProxy
     */
    public synchronized T getItem(Integer id) {
        IdMarkPoolItem<T> compute = idMapItem.compute(id, (k, v) -> {
            if (v == null) return null;

            useCountMap.compute(id, (k1, v1) -> {
                if (v1 == null) v1 = 0;

                return v1 + 1;
            });

            return v;
        });

        if (compute == null) return null;

        return compute.data;
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
                        return (T) getIdMapItem().get(e.getKey()).getData();
                    }).toList();
            res.addAll(batch);

            needCount -= batch.size();
        }

        return res;
    }

    /**
     * 获取全部
     *
     * @return List<T>
     */
    public List<T> getAllItem() {
        return getIdMapItem().values().stream().map(e -> (T) e).toList();
    }


    /**
     * 打印池
     *
     * @return String
     */
    public String printPool() {
        return CommandLineTablePrintHelper.generateTableString(new ArrayList<>(idMapItem.values()), tClass);
    }


    public T buildTInstanceFromLineStr(Object originLine)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<T> constructor = getTClass().getConstructor(Object.class);
        return constructor.newInstance(originLine);
    }


    @Getter
    @Setter
    public static class IdMarkPoolItem<T> {
        private T data;

        private Integer id;

        public IdMarkPoolItem(int id, T item) {
            this.id = id;
            this.data = item;
        }
    }
}
