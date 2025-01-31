package cn.com.helei.bot.core.supporter.persistence;

import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeInvocation;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeProxy;
import cn.com.helei.bot.core.util.DiscardingBlockingQueue;
import cn.com.helei.bot.core.util.FileUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;


@Slf4j
public class AccountPersistenceManager {

    private static final String PERSISTENCE_PATH = "accounts";

    private static final String PERSISTENCE_ACCOUNT_PATTERN = "account-%d.json";

    private static final int PERSISTENCE_CACHE_SIZE = 3;

    private final String botName;

    /**
     * 监听的对象 -》 该对象的root
     */
    private final ConcurrentMap<Object, Object> listenedObjRootMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<Object, Object> originRoot2ProxyMap = new ConcurrentHashMap<>();

    /**
     * root更新队列
     */
    private final ConcurrentMap<Object, DiscardingBlockingQueue<String>> rootUpdateQueueMap = new ConcurrentHashMap<>();

    /**
     * root -> dumpPath
     */
    private final ConcurrentMap<Object, String> rootDumpPathMap = new ConcurrentHashMap<>();

    /**
     * dump数据
     */
    private final DumpDataSupporter dumpDataSupporter = new DumpDataSupporter();

    public AccountPersistenceManager(String botName) {
        this.botName = botName;
    }


    public void init() {
        dumpDataSupporter.startDumpTask();
    }


    /**
     * 持久化保存typedAccountMap
     *
     * @param typedAccountMap typedAccountMap
     */
    public synchronized void persistenceAccountContexts(Map<String, List<AccountContext>> typedAccountMap) throws IOException {
        for (Map.Entry<String, List<AccountContext>> entry : typedAccountMap.entrySet()) {
            String type = entry.getKey();
            List<AccountContext> accountContexts = entry.getValue();

            Path path = Paths.get(getPersistencePath(botName, PERSISTENCE_PATH + File.separator + type));
            if (!Files.exists(path)) Files.createDirectories(path);

            for (AccountContext accountContext : accountContexts) {
                String fileName = String.format(PERSISTENCE_ACCOUNT_PATTERN, accountContext.getAccountBaseInfo().getId());

                saveAccountContext(accountContext, path, fileName);
            }
        }
    }


    /**
     * 加载账户上下文
     *
     * @return PersistenceDto
     */
    public synchronized Map<String, List<AccountContext>> loadAccountContexts() {
        Path path = Paths.get(getPersistencePath(botName, PERSISTENCE_PATH));

        if (!Files.exists(path)) return null;

        Map<String, List<AccountContext>> typedAccountMap = new HashMap<>();

        // Step 1 遍历 accounts 目录x
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, Files::isDirectory)) {
            stream.forEach(dirPath -> {
                if (!Files.exists(dirPath)) return;

                // Step 2 遍历 accounts/xxx 目录里的账户持久化文件
                try (Stream<Path> dirWalk = Files.walk(dirPath)) {

                    List<AccountContext> accountContexts = new ArrayList<>();
                    for (Path filePath : dirWalk.filter(Files::isRegularFile)
                            .filter(p -> p.toString().contains("account-")).toList()) {

                        // 便利解析文件
                        Integer idx = Integer.valueOf(filePath.toString()
                                .split("account-")[1].split(".json")[0]);

                        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()));) {
                            String line = null;
                            StringBuilder sb = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }

                            accountContexts.add(JSONObject.parseObject(sb.toString(), AccountContext.class));
                        }
                    }

                    typedAccountMap.put(dirPath.getFileName().toString(), accountContexts);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return typedAccountMap;
        } catch (Exception e) {
            throw new RuntimeException("读取账户文件失败", e);
        }
    }


    /**
     * 注册持久化监听
     *
     * @param targetList targetList
     * @param <T>        targetType
     */
    public <T> void registerPersistenceListener(List<T> targetList, Function<T, String> getSavePath) {
        targetList.replaceAll(target -> bindPersistenceAnnoListener(target, getSavePath.apply(target)));
    }

    /**
     * 对象属性添加变化监听
     *
     * @param target target 目标对象
     * @param <T>    T
     * @return 添加监听后被动态代理的对象
     */
    public <T> T bindPersistenceAnnoListener(T target, String savePath) {
        rootDumpPathMap.put(target, savePath);
        return doBindPersistenceAnnoListener(target, target);
    }


    /**
     * 对象属性添加变化监听
     *
     * @param target target 目标对象
     * @param <T>    T
     * @return 添加监听后被动态代理的对象
     */
    private <T> T doBindPersistenceAnnoListener(T target, Object rootObj) {
        if (target == null) return null;

        Class<?> targetClass = target.getClass();

        PropertyChangeListenClass propertyChangeListenClass = targetClass.getAnnotation(PropertyChangeListenClass.class);

        // 类上带有PersistenceClass注解，表示可以的类
        if (propertyChangeListenClass == null) {
            return target;
        }

        T proxy = PropertyChangeProxy.createProxy(target, this::propertyChangeHandler);

        // 深度监听，还要给监听的字段对象内的属性监听
        if (propertyChangeListenClass.isDeep()) {

            for (Field field : targetClass.getDeclaredFields()) {
                field.setAccessible(true);
                // 字段上带有PersistenceField注解，表示可以的字段， 字段类型上带有PersistenceClass，还要监听字段对象的属性
                if (field.isAnnotationPresent(PropertyChangeListenField.class)
                        && field.getType().isAnnotationPresent(PropertyChangeListenClass.class)) {
                    try {
                        Object fieldValue = field.get(target);
                        Object filedProxy = doBindPersistenceAnnoListener(fieldValue, rootObj);

                        field.set(target, filedProxy);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("访问字段失败", e);
                    }
                }
            }
        }

        listenedObjRootMap.put(target, rootObj);

        if (target.equals(rootObj)) {
            originRoot2ProxyMap.put(rootObj, proxy);
        }

        return proxy;
    }

    /**
     * 获取账户持久化路径
     *
     * @param botName        botName
     * @param accountContext accountContext
     * @return path
     */
    public static String getAccountContextPersistencePath(String botName, String type, AccountContext accountContext) {
        Path path = Paths.get(getPersistencePath(botName, PERSISTENCE_PATH + File.separator + type));
        String fileName = String.format(PERSISTENCE_ACCOUNT_PATTERN, accountContext.getAccountBaseInfo().getId());

        return Paths.get(path.toString(), fileName).toString();
    }

    /**
     * 属性改变后触发
     *
     * @param invocation invocation
     */
    private void propertyChangeHandler(PropertyChangeInvocation invocation) {
        // 找到root，更新后的root放入队列
        Object root = listenedObjRootMap.get(invocation.getTarget());
        Object rootProxy = originRoot2ProxyMap.get(root);

        rootUpdateQueueMap.compute(root, (k, v) -> {
            log.debug("目标[{}] 属性改变了:{},{}->{} [{}]", root.hashCode(), invocation.getPropertyName(),
                    invocation.getOldValue(), invocation.getNewValue(), invocation.getTimestamp());

            if (v == null) {
                v = new DiscardingBlockingQueue<>(PERSISTENCE_CACHE_SIZE);

                dumpDataSupporter.bindUpdateQueue(rootDumpPathMap.get(root), v);
            }

            try {
                v.put(JSONObject.toJSONString(rootProxy));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return v;
        });
    }


    /**
     * 保存账户
     *
     * @param accountContext accountContext
     * @param path           accountContext
     * @param fileName       fileName
     */
    private static void saveAccountContext(AccountContext accountContext, Path path, String fileName) {
        FileUtil.saveJSONStringContext(Path.of(path.toString(), fileName), JSONObject.toJSONString(accountContext));
    }


    /**
     * 获取持久化路径， 项目根目录开始
     *
     * @param botName botName
     * @param subPath subPath
     * @return String
     */
    public static String getPersistencePath(String botName, String subPath) {
        return FileUtil.RESOURCE_ROOT_DIR + File.separator + "data" + File.separator + botName + File.separator + subPath;
    }
}
