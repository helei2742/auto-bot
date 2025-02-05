package cn.com.helei.bot.core.supporter.persistence.impl;

import cn.com.helei.bot.core.config.TypedAccountConfig;
import cn.com.helei.bot.core.constants.ProxyType;
import cn.com.helei.bot.core.entity.AccountBaseInfo;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.entity.BrowserEnv;
import cn.com.helei.bot.core.supporter.persistence.AbstractPersistenceManager;
import cn.com.helei.bot.core.supporter.persistence.FileDumpDataSupporter;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeInvocation;
import cn.com.helei.bot.core.util.DiscardingBlockingQueue;
import cn.com.helei.bot.core.util.FileUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
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
import java.util.stream.Stream;


@Slf4j
public class FileAccountPersistenceManager extends AbstractPersistenceManager {

    private static final String PERSISTENCE_PATH = "accounts";

    private static final String PERSISTENCE_ACCOUNT_PATTERN = "account-%d.json";

    private static final int PERSISTENCE_CACHE_SIZE = 3;

    /**
     * root -> dumpPath
     */
    private final ConcurrentMap<Object, String> rootDumpPathMap = new ConcurrentHashMap<>();


    private final String botName;

    /**
     * root更新队列
     */
    private final ConcurrentMap<Object, DiscardingBlockingQueue<String>> rootUpdateQueueMap = new ConcurrentHashMap<>();

    /**
     * dump数据
     */
    private final FileDumpDataSupporter fileDumpDataSupporter = new FileDumpDataSupporter();


    public FileAccountPersistenceManager(String botName) {
        this.botName = botName;
    }

    @Override
    public void init() {
        fileDumpDataSupporter.startDumpTask();
    }

    /**
     * 持久化保存typedAccountMap
     *
     * @param typedAccountMap typedAccountMap
     */
    @Override
    public synchronized void persistenceAccountContexts(Map<String, List<AccountContext>> typedAccountMap) {
        for (Map.Entry<String, List<AccountContext>> entry : typedAccountMap.entrySet()) {
            String type = entry.getKey();
            List<AccountContext> accountContexts = entry.getValue();

            Path path = Paths.get(getPersistencePath(botName, PERSISTENCE_PATH + File.separator + type));
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            for (AccountContext accountContext : accountContexts) {
                String fileName = String.format(PERSISTENCE_ACCOUNT_PATTERN, accountContext.getAccountBaseInfo().getId());

                saveAccountContext(accountContext, path, fileName);
            }
        }
    }


    @Override
    public <T> T bindPersistenceAnnoListener(String type, T target) {
        rootDumpPathMap.put(target, getPersistencePath(type, (AccountContext) target));
        return super.bindPersistenceAnnoListener(type, target);
    }


    /**
     * 加载新的账户上下文列表，从配置文件中
     *
     * @return Map<String, List < AccountContext>>
     */
    @Override
    public Map<String, List<AccountContext>> createAccountContexts(Integer projectId, List<TypedAccountConfig> accountConfigs) {

        Map<String, List<AccountContext>> typedAccountContextMap = new HashMap<>();

        // 根据配置加载账号
        for (TypedAccountConfig accountConfig : accountConfigs) {
            String type = accountConfig.getType();

            List<AccountBaseInfo> accountBaseInfos = loadBaseAccountInfoFromFile(type, accountConfig.getAccountFileUserDirPath());
            // Step 1 初始化账号

            // 加载账户配置文件中的主账户
            List<AccountContext> mainAccounts = buildAccountContext(accountConfig.getProxyType(), accountBaseInfos);

            typedAccountContextMap.put(type, mainAccounts);
        }

        return typedAccountContextMap;
    }

    /**
     * 加载账户上下文
     *
     * @return PersistenceDto
     */
    @Override
    public synchronized Map<String, List<AccountContext>> loadAccountContexts(Integer projectId) {
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
     * 属性改变后触发
     *
     * @param invocation invocation
     */
    @Override
    protected void propertyChangeHandler(PropertyChangeInvocation invocation) {
        // 找到root，更新后的root放入队列
        Object root = getListenedObjRootMap().get(invocation.getTarget());
        Object rootProxy = getOriginRoot2ProxyMap().get(root);

        rootUpdateQueueMap.compute(root, (k, v) -> {
            log.debug("目标[{}] 属性改变了:{},{}->{} [{}]", root.hashCode(), invocation.getPropertyName(),
                    invocation.getOldValue(), invocation.getNewValue(), invocation.getTimestamp());

            if (v == null) {
                v = new DiscardingBlockingQueue<>(PERSISTENCE_CACHE_SIZE);

                fileDumpDataSupporter.bindUpdateQueue(rootDumpPathMap.get(root), v);
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
     * 构建depinClientAccounts
     *
     * @param accountBaseInfos depinClientAccounts
     * @return AccountContext
     */
    private List<AccountContext> buildAccountContext(ProxyType proxyType, List<AccountBaseInfo> accountBaseInfos) {
        List<AccountContext> newAccountContexts = new ArrayList<>();
        for (AccountBaseInfo accountBaseInfo : accountBaseInfos) {
            AccountContext accountContext = new AccountContext();
        }

        return newAccountContexts;
    }

    /**
     * 从文件加载账户基础信息
     *
     * @param type                   type
     * @param accountFileUserDirPath accountFileUserDirPath
     * @return List<AccountBaseInfo>
     */
    private List<AccountBaseInfo> loadBaseAccountInfoFromFile(String type, String accountFileUserDirPath) {
        String resourcePath = FileUtil.getConfigDirResourcePath(List.of(), accountFileUserDirPath);

        try (BufferedReader reader = new BufferedReader(new FileReader(resourcePath))) {
            List<AccountBaseInfo> accountBaseInfos = new ArrayList<>();

            String line;
            int id = 0;
            while ((line = reader.readLine()) != null) {
                AccountBaseInfo baseInfo = new AccountBaseInfo(line);
                baseInfo.setId(id++);
                baseInfo.setType(type);
                accountBaseInfos.add(baseInfo);
            }

            return accountBaseInfos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
     * 获取账户持久化路径
     *
     * @param accountContext accountContext
     * @return path
     */
    private String getPersistencePath(String type, AccountContext accountContext) {
        Path path = Paths.get(getPersistencePath(botName, PERSISTENCE_PATH + File.separator + type));
        String fileName = String.format(PERSISTENCE_ACCOUNT_PATTERN, accountContext.getAccountBaseInfo().getId());

        return Paths.get(path.toString(), fileName).toString();
    }


    /**
     * 获取持久化路径， 项目根目录开始
     *
     * @param botName botName
     * @param subPath subPath
     * @return String
     */
    private static String getPersistencePath(String botName, String subPath) {
        return FileUtil.RESOURCE_ROOT_DIR + File.separator + "data" + File.separator + botName + File.separator + subPath;
    }
}
