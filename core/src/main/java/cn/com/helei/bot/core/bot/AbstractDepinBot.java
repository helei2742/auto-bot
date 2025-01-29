package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.config.BaseDepinBotConfig;
import cn.com.helei.bot.core.constants.DepinBotStatus;
import cn.com.helei.bot.core.dto.DepinBotRuntimeInfo;
import cn.com.helei.bot.core.pool.account.AccountPool;
import cn.com.helei.bot.core.pool.env.BrowserEnvPool;
import cn.com.helei.bot.core.exception.DepinBotInitException;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.exception.DepinBotStatusException;
import cn.com.helei.bot.core.pool.network.DynamicProxyPool;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.com.helei.bot.core.pool.network.StaticProxyPool;
import cn.com.helei.bot.core.pool.twitter.TwitterPool;
import cn.com.helei.bot.core.util.NamedThreadFactory;
import cn.com.helei.bot.core.util.RestApiClientFactory;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
@Getter
public abstract class AbstractDepinBot {
    /**
     * 执行异步任务的线程池
     */
    private final ExecutorService executorService;

    /**
     * 静态代理池
     */
    private final StaticProxyPool staticProxyPool;

    /**
     * 动态代理池
     */
    private final DynamicProxyPool dynamicProxyPool;

    /**
     * 浏览器环境池
     */
    private final BrowserEnvPool browserEnvPool;

    /**
     * 账户池
     */
    private final AccountPool accountPool;

    /**
     * 推特池
     */
    private final TwitterPool twitterPool;

    /**
     * 配置
     */
    private final BaseDepinBotConfig baseDepinBotConfig;

    /**
     * 状态
     */
    private DepinBotStatus status = DepinBotStatus.NEW;

    /**
     * 代理并发控制
     */
    private final Map<NetworkProxy, Semaphore> networkSyncControllerMap;

    /**
     * bot运行时信息
     */
    private final DepinBotRuntimeInfo depinBotRuntimeInfo;

    public AbstractDepinBot(BaseDepinBotConfig baseDepinBotConfig) {
        if (StrUtil.isBlank(baseDepinBotConfig.getName())) throw new IllegalArgumentException("bot 名字不能为空");

        this.baseDepinBotConfig = baseDepinBotConfig;
        this.executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory(baseDepinBotConfig.getName() + "-executor"));

        this.staticProxyPool = StaticProxyPool.loadYamlPool(
                baseDepinBotConfig.getStaticPoolConfig(),
                "bot.network.proxy-static",
                StaticProxyPool.class
        );
        this.dynamicProxyPool = DynamicProxyPool.loadYamlPool(
                baseDepinBotConfig.getDynamicProxyConfig(),
                "bot.network.proxy-dynamic",
                DynamicProxyPool.class
        );
        this.browserEnvPool = BrowserEnvPool.loadYamlPool(
                baseDepinBotConfig.getBrowserEnvPoolConfig(),
                "bot.browser",
                BrowserEnvPool.class
        );
        this.accountPool = AccountPool.loadYamlPool(
                baseDepinBotConfig.getAccountPoolConfig(),
                "bot.account",
                AccountPool.class
        );
        this.twitterPool = TwitterPool.loadYamlPool(
                baseDepinBotConfig.getTwitterPoolConfig(),
                "bot.twitter",
                TwitterPool.class
        );

        this.networkSyncControllerMap = new ConcurrentHashMap<>();

        this.depinBotRuntimeInfo = new DepinBotRuntimeInfo();
    }

    public void init() {
        updateState(DepinBotStatus.INIT);
        try {
            doInit();

            //更新状态
            updateState(DepinBotStatus.INIT_FINISH);
        } catch (Exception e) {
            log.error("初始化DepinBot[{}}发生错误", getBaseDepinBotConfig().getName(), e);
            updateState(DepinBotStatus.INIT_ERROR);
        }
    }

    /**
     * 初始化方法
     */
    protected abstract void doInit() throws DepinBotInitException;

    /**
     * 机器人运行方法
     *
     * @throws IOException IOException
     */
    protected abstract void doExecute() throws IOException;

    /**
     * 启动bot
     *
     * @throws DepinBotStartException DepinBotStartException
     */
    public void start() throws DepinBotStartException {
        updateState(DepinBotStatus.STARTING);
        log.info("正在启动Depin Bot");
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            //启动命令行交互的线程
            asyncExecute(startLatch);

            log.info("Depin Bot启动完毕");

            updateState(DepinBotStatus.RUNNING);
            startLatch.await();

        } catch (Exception e) {
            updateState(DepinBotStatus.SHUTDOWN);
            throw new DepinBotStartException("启动CommandLineDepinBot发生错误", e);
        }
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
     * @return CompletableFuture<String> response str
     */
    public CompletableFuture<String> syncRequest(
            NetworkProxy proxy,
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
            NetworkProxy proxy,
            String url,
            String method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            Supplier<String> requestStart
    ) {

        Semaphore networkController = networkSyncControllerMap.compute(proxy, (k, v) -> {
            if (v == null) {
                v = new Semaphore(baseDepinBotConfig.getConcurrentCount());
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

                return RestApiClientFactory.getClient(proxy).request(
                        url,
                        method,
                        headers,
                        params,
                        body
                ).get();
            } catch (InterruptedException | ExecutionException e) {
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
        getDepinBotRuntimeInfo().getKeyValueInfoMap().forEach((k, v) -> {
            sb.append(k).append(": ").append(v).append("\n");
        });
        return sb.toString();
    }


    /**
     * 异步启动
     */
    private void asyncExecute(CountDownLatch startLatch) {
        Thread commandInputThread = new Thread(() -> {
            try {
                doExecute();
            } catch (IOException e) {
                log.error("启动bot发生错误", e);
            } finally {
                startLatch.countDown();
            }
        }, "depin-bot-main");
        commandInputThread.setDaemon(true);
        commandInputThread.start();
    }


    /**
     * 更新DepinBotStatus
     *
     * @param newStatus 新状态
     */
    private synchronized void updateState(DepinBotStatus newStatus) throws DepinBotStatusException {
        boolean b = true;
        if (newStatus.equals(DepinBotStatus.SHUTDOWN)) {
            status = DepinBotStatus.SHUTDOWN;
            b = false;
        } else {
            b = switch (status) {
                //当前为NEW，新状态才能为NEW,SHUTDOWN
                case NEW -> DepinBotStatus.INIT.equals(newStatus);
                //当前为INIT，新状态只能为INIT_FINISH、INIT_ERROR,SHUTDOWN
                case INIT -> newStatus.equals(DepinBotStatus.INIT_FINISH)
                        || newStatus.equals(DepinBotStatus.INIT_ERROR);
                //当前为INIT_ERROR,新状态只能为ACCOUNT_LOADING, SHUTDOWN
                case INIT_ERROR -> newStatus.equals(DepinBotStatus.INIT);
                //当前状态为INIT_FINISH，新状态只能为ACCOUNT_LIST_CONNECT, SHUTDOWN
                case INIT_FINISH -> newStatus.equals(DepinBotStatus.STARTING);
                //当前状态为STARING，新状态只能为RUNNING,SHUTDOWN
                case STARTING -> newStatus.equals(DepinBotStatus.RUNNING);
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
}
