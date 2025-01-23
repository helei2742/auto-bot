package cn.com.helei.DepinBot.core.bot;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.constants.DepinBotStatus;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.pool.account.AccountPool;
import cn.com.helei.DepinBot.core.pool.env.BrowserEnvPool;
import cn.com.helei.DepinBot.core.exception.DepinBotInitException;
import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import cn.com.helei.DepinBot.core.exception.DepinBotStatusException;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxyPool;
import cn.com.helei.DepinBot.core.util.NamedThreadFactory;
import cn.com.helei.DepinBot.core.util.RestApiClientFactory;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
@Getter
public abstract class AbstractDepinBot<Req, Resp> {
    /**
     * 执行异步任务的线程池
     */
    private final ExecutorService executorService;

    /**
     * 代理池
     */
    private final NetworkProxyPool proxyPool;

    /**
     * 浏览器环境池
     */
    private final BrowserEnvPool browserEnvPool;

    /**
     * 账户池
     */
    private final AccountPool accountPool;

    /**
     * 配置
     */
    private final BaseDepinBotConfig baseDepinBotConfig;

    /**
     * 状态
     */
    private DepinBotStatus status = DepinBotStatus.NEW;

    /**
     * 同步控制
     */
    private final Semaphore syncController;

    public AbstractDepinBot(BaseDepinBotConfig baseDepinBotConfig) {
        this.baseDepinBotConfig = baseDepinBotConfig;
        this.executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory(baseDepinBotConfig.getName() + "-executor"));

        this.proxyPool = NetworkProxyPool.loadYamlPool(
                baseDepinBotConfig.getNetworkPoolConfig(),
                "bot.network.proxy",
                NetworkProxyPool.class
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

        syncController = new Semaphore(baseDepinBotConfig.getConcurrentCount());
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


    protected abstract void doExecute() throws IOException;

    /**
     * 使用accountContext构建AbstractDepinWSClient
     *
     * @param accountContext accountContext
     * @return AbstractDepinWSClient
     */
    public abstract BaseDepinWSClient<Req, Resp> buildAccountWSClient(AccountContext accountContext);


    /**
     * 当账户链接时调用
     *
     * @param depinWSClient depinWSClient
     * @param success       是否成功
     */
    public abstract void whenAccountConnected(BaseDepinWSClient<Req, Resp> depinWSClient, Boolean success);

    /**
     * 当ws连接收到响应
     *
     * @param depinWSClient depinWSClient
     * @param id            id
     * @param response      response
     */
    public abstract void whenAccountReceiveResponse(BaseDepinWSClient<Req, Resp> depinWSClient, String id, Resp response);

    /**
     * 当ws连接收到消息
     *
     * @param depinWSClient depinWSClient
     * @param message       message
     */
    public abstract void whenAccountReceiveMessage(BaseDepinWSClient<Req, Resp> depinWSClient, Resp message);

    /**
     * 获取心跳消息
     *
     * @param depinWSClient depinWSClient
     * @return 消息体
     */
    public abstract Req getHeartbeatMessage(BaseDepinWSClient<Req, Resp> depinWSClient);


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
     * @param body    body
     * @param params  params
     * @return CompletableFuture<String> response str
     */
    public CompletableFuture<Response> syncRequest(
            NetworkProxy proxy,
            String url,
            String method,
            Map<String, String> headers,
            JSONObject body,
            JSONObject params
    ) {
        return syncRequest(proxy, url, method, headers, body, params, null);
    }

    /**
     * 同步请求，使用syncController控制并发
     *
     * @param proxy   proxy
     * @param url     url
     * @param method  method
     * @param headers headers
     * @param body    body
     * @param params  params
     * @return CompletableFuture<Response> response
     */
    public CompletableFuture<Response> syncRequest(
            NetworkProxy proxy,
            String url,
            String method,
            Map<String, String> headers,
            JSONObject params,
            JSONObject body,
            Supplier<String> requestStart
    ) {
        return CompletableFuture.supplyAsync(()->{
            try {
                syncController.acquire();
                // 随机延迟
                TimeUnit.MILLISECONDS.sleep(RandomUtil.randomLong(1000, 3000));

                String str = "开始";
                if (requestStart != null) {
                    str = requestStart.get();
                }
                log.debug("同步器允许发送请求-{}", str);

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
                syncController.release();
            }
        }, executorService);
    }

    /**
     * 添加定时任务
     *
     * @param runnable runnable
     * @param delay    delay
     * @param timeUnit timeUnit
     */
    public void addTimer(Runnable runnable, long delay, TimeUnit timeUnit) {
        executorService.execute(() -> {
            while (true) {
                try {
                    syncController.acquire();

                    runnable.run();

                    timeUnit.sleep(delay);
                } catch (InterruptedException e) {
                    log.error("timer interrupted will stop it", e);
                    break;
                } finally {
                    syncController.release();
                }
            }
        });
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
