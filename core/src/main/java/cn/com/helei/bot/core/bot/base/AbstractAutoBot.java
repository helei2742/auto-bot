package cn.com.helei.bot.core.bot.base;

import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.bot.constants.BotStatus;
import cn.com.helei.bot.core.dto.AutoBotRuntimeInfo;
import cn.com.helei.bot.core.entity.BotInfo;
import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.util.exception.DepinBotInitException;
import cn.com.helei.bot.core.util.exception.DepinBotStatusException;
import cn.com.helei.bot.core.util.FileUtil;
import cn.com.helei.bot.core.util.NamedThreadFactory;
import cn.com.helei.bot.core.util.RestApiClientFactory;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
@Getter
public abstract class AbstractAutoBot {

    private static final ProxyInfo DEFAULT_PROXY = new ProxyInfo();

    /**
     * 执行异步任务的线程池
     */
    private final ExecutorService executorService;

    /**
     * 配置
     */
    private final AutoBotConfig autoBotConfig;

    /**
     * 状态
     */
    private BotStatus status = BotStatus.NEW;

    /**
     * 代理并发控制
     */
    private final Map<ProxyInfo, Semaphore> networkSyncControllerMap;

    /**
     * bot运行时信息
     */
    private final AutoBotRuntimeInfo autoBotRuntimeInfo;

    /**
     * 请求并发数量
     */
    @Getter
    @Setter
    private int requestConcurrentCount = 5;

    /**
     * bot api
     */
    @Getter
    private final BotApi botApi;

    /**
     * bot信息
     */
    @Getter
    private final BotInfo botInfo;

    public AbstractAutoBot(
            AutoBotConfig autoBotConfig,
            BotApi botApi
    ) {
        if (StrUtil.isBlank(autoBotConfig.getBotKey())) {
            throw new IllegalArgumentException("bot key不能为空");
        }

        this.autoBotConfig = autoBotConfig;
        this.botApi = botApi;
        this.botInfo = buildBotInfo();

        this.networkSyncControllerMap = new ConcurrentHashMap<>();
        this.autoBotRuntimeInfo = new AutoBotRuntimeInfo();

        this.executorService = Executors.newThreadPerTaskExecutor(
                new NamedThreadFactory(botInfo.getName() + "-executor"));
    }


    public void init() {
        updateState(BotStatus.INIT);
        try {
            doInit();

            //更新状态
            updateState(BotStatus.INIT_FINISH);
        } catch (Exception e) {
            log.error("初始化Bot[{}]发生错误", botInfo.getName(), e);
            updateState(BotStatus.INIT_ERROR);
        }
    }


    /**
     * 初始化方法
     */
    protected abstract void doInit() throws DepinBotInitException;


    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<String> response str
     */
    public CompletableFuture<String> syncRequest(
            ProxyInfo proxy,
            String url,
            String method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body
    ) {
        return syncRequest(proxy, url, method, headers, params, body, null);
    }

    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<Response> String
     */
    public CompletableFuture<String> syncRequest(
            ProxyInfo proxy,
            String url,
            String method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            Supplier<String> requestStart
    ) {
        return syncCCHandler(proxy, requestStart, () -> {
            try {
                return RestApiClientFactory.getClient(proxy).request(
                        url,
                        method,
                        headers,
                        params,
                        body
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param params  params
     * @param body    body
     * @return CompletableFuture<Response> String
     */
    public CompletableFuture<List<String>> syncStreamRequest(
            ProxyInfo proxy,
            String url,
            String method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            Supplier<String> requestStart
    ) {
        return syncCCHandler(proxy, requestStart, () -> {
            try {
                return RestApiClientFactory.getClient(proxy).streamRequest(
                        url,
                        method,
                        headers,
                        params,
                        body
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <R> CompletableFuture<R> syncCCHandler(
            ProxyInfo proxy,
            Supplier<String> requestStart,
            Supplier<R> request
    ) {

        Semaphore networkController = networkSyncControllerMap
                .compute(proxy == null ? DEFAULT_PROXY : proxy, (k, v) -> {
                    if (v == null) {
                        v = new Semaphore(requestConcurrentCount);
                    }
                    return v;
                });


        return CompletableFuture.supplyAsync(() -> {
            try {
                networkController.acquire();
                // 随机延迟
                TimeUnit.MILLISECONDS.sleep(RandomUtil.randomLong(1000, 3000));

                String str = "开始";
                if (requestStart != null) {
                    str = requestStart.get();
                }
                log.info("同步器允许发送请求-{}", str);

                return request.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                networkController.release();
            }
        }, executorService);
    }

    /**
     * 打印BotRuntimeInfo
     *
     * @return string
     */
    public String printBotRuntimeInfo() {
        StringBuilder sb = new StringBuilder();
        getAutoBotRuntimeInfo().getKeyValueInfoMap().forEach((k, v) -> {
            sb.append(k).append(": ").append(v).append("\n");
        });
        return sb.toString();
    }

    /**
     * 获取app的配置目录
     *
     * @return String
     */
    public String getAppConfigDir() {
        return FileUtil.getConfigDirResourcePath(SystemConfig.CONFIG_DIR_APP_PATH, botInfo.getName());
    }


    /**
     * 更新BotStatus
     *
     * @param newStatus 新状态
     */
    public synchronized void updateState(BotStatus newStatus) throws DepinBotStatusException {
        boolean b = true;
        if (newStatus.equals(BotStatus.SHUTDOWN)) {
            status = BotStatus.SHUTDOWN;
            b = false;
        } else {
            b = switch (status) {
                //当前为NEW，新状态才能为NEW,SHUTDOWN
                case NEW -> BotStatus.INIT.equals(newStatus);
                //当前为INIT，新状态只能为INIT_FINISH、INIT_ERROR,SHUTDOWN
                case INIT -> newStatus.equals(BotStatus.INIT_FINISH)
                        || newStatus.equals(BotStatus.INIT_ERROR);
                //当前为INIT_ERROR,新状态只能为ACCOUNT_LOADING, SHUTDOWN
                case INIT_ERROR -> newStatus.equals(BotStatus.INIT);
                //当前状态为INIT_FINISH，新状态只能为ACCOUNT_LIST_CONNECT, SHUTDOWN
                case INIT_FINISH -> newStatus.equals(BotStatus.STARTING);
                //当前状态为STARING，新状态只能为RUNNING,SHUTDOWN
                case STARTING -> newStatus.equals(BotStatus.RUNNING);
                //RUNNING，新状态只能为 SHUTDOWN
                case RUNNING -> false;
                case SHUTDOWN -> false;
            };
        }


        if (b) {
            log.info("CommandLineDepinBot Status change [{}] => [{}]", status, newStatus);
            this.status = newStatus;
        } else {
            throw new DepinBotStatusException(String.format("Depin Bot Status不能从[%s]->[%s]", status, newStatus));
        }
    }


    protected abstract BotInfo buildBotInfo();
}
