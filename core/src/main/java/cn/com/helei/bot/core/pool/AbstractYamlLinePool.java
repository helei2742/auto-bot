package cn.com.helei.bot.core.pool;


import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import cn.com.helei.bot.core.util.table.CommandLineTablePrintHelper;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class AbstractYamlLinePool<T extends AbstractYamlLineItem> {

    private final Class<T> tClass;

    @Getter
    @Setter
    private List<Object> list;

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
            String dirPath, String path, Class<C> cClass) {

        C pool = YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_BOT_PATH, dirPath, Arrays.stream(path.split("\\.")).toList(), cClass);
        pool.setConfigClassPath(dirPath);

        List<Object> list1 = pool.getList();

        if (list1 == null || list1.isEmpty()) {return pool;}

        AtomicInteger id = new AtomicInteger();
        for (Object rawLine : list1) {
            AbstractYamlLineItem item;

            try {
                item = pool.buildTInstanceFromLineStr(rawLine);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            int itemID = id.getAndIncrement();
            item.setId(itemID);

            pool.itemCreatedHandler(item);

            pool.getIdMapItem().put(itemID, item);
            pool.getUseCountMap().put(itemID, 0);
        }


        return pool;
    }

    protected void itemCreatedHandler(AbstractYamlLineItem item) {
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
        if (getIdMapItem().isEmpty()) return Collections.emptyList();

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
     * 查询未使用的id
     *
     * @return List<T>
     */
    public synchronized List<Integer> getUnUsedItemId() {
       return getUseCountMap().entrySet().stream()
                .filter(e -> e.getValue() == 0).map(Map.Entry::getKey).toList();
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


    public int size() {
        return idMapItem.size();
    }
}
