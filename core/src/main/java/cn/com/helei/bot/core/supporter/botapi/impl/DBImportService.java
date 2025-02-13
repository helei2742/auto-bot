package cn.com.helei.bot.core.supporter.botapi.impl;

import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.constants.ProxyProtocol;
import cn.com.helei.bot.core.constants.ProxyType;
import cn.com.helei.bot.core.entity.*;
import cn.com.helei.bot.core.mvc.service.*;
import cn.com.helei.bot.core.mvc.service.ITelegramAccountService;
import cn.com.helei.bot.core.supporter.botapi.ImportService;
import cn.com.helei.bot.core.util.excel.ExcelReadUtil;
import cn.com.helei.bot.core.util.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static cn.com.helei.bot.core.constants.MapConfigKey.USER_AGENT_KEY;

@Slf4j
@Component
public class DBImportService implements ImportService {

    public static final String DEFAULT_ACCOUNT_TYPE = "default";

    @Autowired
    @Lazy
    private IBotAccountContextService botAccountContextService;

    @Autowired
    @Lazy
    private IAccountBaseInfoService accountBaseInfoService;

    @Autowired
    @Lazy
    private IProxyInfoService proxyInfoService;

    @Autowired
    @Lazy
    private IBrowserEnvService browserEnvService;

    @Autowired
    @Lazy
    private ITwitterAccountService twitterAccountService;

    @Autowired
    @Lazy
    private IDiscordAccountService discordAccountService;

    @Autowired
    @Lazy
    private ITelegramAccountService telegramAccountService;


    @Override
    public Integer importBotAccountContextFromExcel(Integer botId, String botKey, String fileBotConfigPath) {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_APP_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(proxyFilePath);

            return importBotAccountContextFromRaw(botId, botKey, rawLines);
        } catch (Exception e) {
            log.error("读取twitter account 文件[{}]发生异常", proxyFilePath, e);
            return 0;
        }
    }

    @Override
    public Integer importBotAccountContextFromRaw(Integer botId, String botKey, List<Map<String, Object>> rawLines) {
        List<AccountContext> accountContexts = rawLines.stream().map(map -> {
                    Integer customBotId = autoCast(map.remove("bot_id"));
                    String customBotKey = autoCast(map.remove("bot_key"));

                    return AccountContext.builder()
                            .botId(customBotId == null ? botId : customBotId)
                            .botKey(StrUtil.isBlank(customBotKey) ? botKey : customBotKey)
                            .accountBaseInfoId(toInteger(map.remove("account_base_info_id")))
                            .twitterId(toInteger(map.remove("twitter_id")))
                            .discordId(toInteger(map.remove("discord_id")))
                            .proxyId(toInteger((map.remove("proxy_id"))))
                            .browserEnvId(toInteger(map.remove("browser_env_id")))
                            .telegramId(toInteger(map.remove("telegram_id")))
                            .walletId(toInteger(map.remove("wallet_id")))
                            .params(map)
                            .build();
                })
                .toList();

        // 没设置代理的根据配置填充代理
//            tryFillProxy(accountContexts, proxyRepeat, proxyType);

        // 没设置浏览器环境的根据设置填充环境
        tryFillBrowserEnv(accountContexts);

        return botAccountContextService.insertOrUpdateBatch(accountContexts);
    }


    @Override
    public Integer importBrowserEnvFromExcel(String fileBotConfigPath) {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        List<Map<String, Object>> headerList = ExcelReadUtil.readExcelToMap(proxyFilePath);

        log.info("文件解析成功, 共[{}]个", headerList.size());
        return importProxyFromRaw(headerList);
    }

    @Override
    public Integer importBrowserEnvFromRaw(List<Map<String, Object>> rawLines) {

        List<BrowserEnv> list = rawLines.stream().map(map -> {
            Object userAgent = map.remove(USER_AGENT_KEY);
            if (userAgent == null) return null;

            BrowserEnv browserEnv = new BrowserEnv();
            browserEnv.setUserAgent((String) userAgent);
            browserEnv.setOtherHeader(map);
            return browserEnv;
        }).filter(Objects::nonNull).toList();

        return browserEnvService.insertOrUpdateBatch(list);
    }

    @Override
    public Integer importProxyFromExcel(String botConfigPath) {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, botConfigPath);

        int total = 0;

        try {
            List<Map<String, Object>> staticProxies = ExcelReadUtil.readExcelToMap(proxyFilePath, "static");

            total += importProxyFromRaw(staticProxies);

            List<Map<String, Object>> dynamicProxies = ExcelReadUtil.readExcelToMap(proxyFilePath, "dynamic");

            total += importProxyFromRaw(dynamicProxies);

            log.info("代理配置文件解析成功，static-proxy:[{}], dynamic-proxy:[{}]", staticProxies.size(), dynamicProxies.size());

            return total;
        } catch (Exception e) {
            log.error("解析代理配置文件[{}]错误", proxyFilePath, e);

            return 0;
        }
    }

    @Override
    public Integer importProxyFromRaw(List<Map<String, Object>> rawLines) {

        List<ProxyInfo> list = rawLines.stream().map(map -> {
            String proxyProtocol = autoCast(map.remove("proxy_protocol"));

            return ProxyInfo.builder()
                    .proxyType(ProxyType.STATIC)
                    .host(autoCast(map.remove("host")))
                    .port(Integer.valueOf(autoCast(map.remove("port"))))
                    .proxyProtocol(ProxyProtocol.valueOf(proxyProtocol.toUpperCase()))
                    .username(autoCast(map.remove("username")))
                    .password(autoCast(map.remove("password")))
                    .params(map)
                    .build();
        }).toList();

        return proxyInfoService.insertOrUpdateBatch(list);
    }

    @Override
    public Map<String, Integer> importAccountBaseInfoFromExcel(String botConfigPath) {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, botConfigPath);
        Map<String, Integer> result = new HashMap<>();

        try {
            ExcelReadUtil.readExcelAsMap(
                    proxyFilePath,
                    (type, map) -> AccountBaseInfo.builder()
                            .type(type)
                            .name(autoCast(map.remove("name")))
                            .email(autoCast(map.remove("email")))
                            .password(autoCast(map.remove("password")))
                            .params(map)
                            .build(),
                    (type, accountBaseInfos) -> CompletableFuture.runAsync(() -> {
                        log.info("[{}] 账号基本信息读取完毕, 共[{}]", type, accountBaseInfos.size());
                        Integer insertCount = accountBaseInfoService.insertOrUpdateBatch(accountBaseInfos);

                        log.info("[{}] 账号基本信息保存成功, 新增[{}], 共[{}]", type, insertCount, accountBaseInfos.size());

                        result.put(type, accountBaseInfos.size());
                    })
            );

        } catch (IOException e) {
            log.error("从文件导入账号基本信息出错", e);
        }

        return result;
    }

    @Override
    public Integer importAccountBaseInfoFromRaw(List<Map<String, Object>> rawLines) {
        List<AccountBaseInfo> list = rawLines.stream().map(map -> {
            String type = autoCast(map.remove("type"));

            return AccountBaseInfo.builder()
                    .type(StrUtil.isBlank(type) ? DEFAULT_ACCOUNT_TYPE : type)
                    .name(autoCast(map.remove("name")))
                    .email(autoCast(map.remove("email")))
                    .password(autoCast(map.remove("password")))
                    .params(map)
                    .build();
        }).toList();

        return accountBaseInfoService.insertOrUpdateBatch(list);
    }

    @Override
    public Integer importTwitterFromExcel(String fileBotConfigPath) {
        String proxyFilePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(proxyFilePath);

            return importTwitterFromRaw(rawLines);
        } catch (Exception e) {
            log.error("读取twitter account 文件[{}]发生异常", proxyFilePath, e);
            return 0;
        }
    }

    @Override
    public Integer importTwitterFromRaw(List<Map<String, Object>> rawLines) {
        List<TwitterAccount> twitterAccounts = rawLines.stream().map(map -> TwitterAccount.builder()
                .username(autoCast(map.remove("username")))
                .password(autoCast(map.remove("password")))
                .email(autoCast(map.remove("email")))
                .emailPassword(autoCast(map.remove("email_password")))
                .token(autoCast(map.remove("token")))
                .f2aKey(autoCast(map.remove("f2a_key")))
                .params(map)
                .build()).toList();

        return twitterAccountService.insertOrUpdateBatch(twitterAccounts);
    }

    @Override
    public Integer importDiscordFromExcel(String fileBotConfigPath) {
        String dirResourcePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(dirResourcePath);

            return importDiscordFromRaw(rawLines);
        } catch (Exception e) {
            log.error("读取discord account 文件[{}]发生异常", dirResourcePath, e);
            return 0;
        }
    }

    @Override
    public Integer importDiscordFromRaw(List<Map<String, Object>> rawLines) {
        List<DiscordAccount> discordAccounts = rawLines.stream().map(map -> DiscordAccount.builder()
                .username(autoCast(map.remove("username")))
                .password(autoCast(map.remove("password")))
                .bindEmail(autoCast(map.remove("bind_email")))
                .bindEmailPassword(autoCast(map.remove("bind_email_password")))
                .token(autoCast(map.remove("token")))
                .params(map)
                .build()
        ).toList();

        return discordAccountService.insertOrUpdateBatch(discordAccounts);
    }

    @Override
    public Integer importTelegramFormExcel(String fileBotConfigPath) {
        String dirResourcePath = FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_BOT_PATH, fileBotConfigPath);

        try {
            List<Map<String, Object>> rawLines = ExcelReadUtil.readExcelToMap(dirResourcePath);

            return importTelegramFormRaw(rawLines);
        } catch (Exception e) {
            log.error("读取telegram account 文件[{}]发生异常", dirResourcePath, e);
            return 0;
        }
    }

    @Override
    public Integer importTelegramFormRaw(List<Map<String, Object>> rawLines) {

        List<TelegramAccount> telegramAccounts = rawLines.stream().map(map -> TelegramAccount.builder()
                .username(autoCast(map.remove("username")))
                .password(autoCast(map.remove("password")))
                .phonePrefix(autoCast(map.remove("phone_prefix")))
                .phone(autoCast(map.remove("phone")))
                .token(autoCast(map.remove("token")))
                .params(map)
                .build()
        ).toList();


        return telegramAccountService.insertOrUpdateBatch(telegramAccounts);
    }


    private static <T> T autoCast(Object obj) {
        return obj == null ? null : (T) obj;
    }

    private static Integer toInteger(Object obj) {
        return obj == null ? null : Integer.valueOf(obj.toString());
    }

    /**
     * 填充代理
     *
     * @param accountContexts accountContexts
     * @param proxyType       proxyType
     */
    private void tryFillProxy(List<AccountContext> accountContexts, Boolean proxyRepeat, ProxyType proxyType) {
        ;// 创建 Page 对象，设置当前页和每页的条数

        Page<ProxyInfo> page = new Page<>(1, accountContexts.size());
        QueryWrapper<ProxyInfo> queryWrapper = new QueryWrapper<>(ProxyInfo.builder().proxyType(proxyType).build())
                .select("id");

        List<ProxyInfo> allProxy = proxyInfoService.list(page, queryWrapper);


        // 筛选
        Map<Integer, Integer> proxyUseCount = new HashMap<>();
        Map<Integer, ProxyInfo> idMapProxy = allProxy.stream().collect(Collectors.toMap(ProxyInfo::getId, p -> {
            proxyUseCount.put(p.getId(), 0);
            return p;
        }));

        Set<AccountContext> noProxyAccounts = new HashSet<>();

        accountContexts.forEach(accountContext -> {

            Integer proxyId = accountContext.getProxyId();

            if (proxyId == null || !idMapProxy.containsKey(proxyId)) {
                // 代理配置无效，给他添上
                noProxyAccounts.add(accountContext);
            } else if (proxyUseCount.getOrDefault(proxyId, 0) > 1 && BooleanUtil.isFalse(proxyRepeat)) {
                // 代理被使用过,并且不允许重复使用
                noProxyAccounts.add(accountContext);
            } else {
                proxyUseCount.put(proxyId, proxyUseCount.getOrDefault(proxyId, 0) + 1);
            }
        });


        // 填充代理
        List<Integer> ids = getLessUsedItem(proxyUseCount, noProxyAccounts.size());
        ArrayList<AccountContext> list = new ArrayList<>(noProxyAccounts);
        for (int i = 0; i < ids.size(); i++) {
            list.get(i).setProxyId(ids.get(i));
        }
    }


    /**
     * 填充浏览器环境
     *
     * @param accountContexts accountContexts
     */
    private void tryFillBrowserEnv(List<AccountContext> accountContexts) {
        Page<BrowserEnv> page = new Page<>(1, accountContexts.size());
        QueryWrapper<BrowserEnv> queryWrapper = new QueryWrapper<BrowserEnv>().select("id");

        List<BrowserEnv> allProxy = browserEnvService.list(page, queryWrapper);

        Map<Integer, Integer> useCount = new HashMap<>();
        Map<Integer, BrowserEnv> idMapEnv = allProxy.stream().collect(Collectors.toMap(BrowserEnv::getId, p -> {
            useCount.put(p.getId(), 0);
            return p;
        }));


        Set<AccountContext> noUseAccounts = new HashSet<>();

        accountContexts.forEach(accountContext -> {
            Integer browserEnvId = accountContext.getBrowserEnvId();

            if (browserEnvId == null || !idMapEnv.containsKey(browserEnvId)) {
                // 配置无效，给他添上
                noUseAccounts.add(accountContext);
            } else {
                useCount.put(browserEnvId, useCount.getOrDefault(browserEnvId, 0) + 1);
            }
        });

        // 填充浏览器环境
        List<Integer> ids = getLessUsedItem(useCount, noUseAccounts.size());
        ArrayList<AccountContext> list = new ArrayList<>(noUseAccounts);
        for (int i = 0; i < ids.size(); i++) {
            list.get(i).setBrowserEnvId(ids.get(i));
        }
    }


    /**
     * 获取最少使用的
     *
     * @param count 数量
     * @return List<T>
     */
    private List<Integer> getLessUsedItem(Map<Integer, Integer> useCountMap, int count) {
        if (useCountMap == null || useCountMap.isEmpty()) return Collections.emptyList();
        int batchSize = Math.min(count, useCountMap.size());

        List<Integer> res = new ArrayList<>(count);

        int needCount = count;
        while (needCount > 0) {
            int currentSize = Math.min(needCount, batchSize);

            List<Integer> batch = useCountMap.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                    .limit(currentSize)
                    .map(e -> {
                        useCountMap.compute(e.getKey(), (k, v) -> v == null ? 0 : v + 1);
                        return e.getKey();
                    }).toList();
            res.addAll(batch);

            needCount -= batch.size();
        }

        return res;
    }

}
