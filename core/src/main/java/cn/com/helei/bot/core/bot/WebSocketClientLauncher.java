package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.bot.anno.BotWSMethodConfig;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.job.AutoBotJobParam;
import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;
import cn.com.helei.bot.core.supporter.netty.constants.WebsocketClientStatus;
import cn.com.helei.bot.core.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;


@Slf4j
public class WebSocketClientLauncher {

    public static final Method lanuchMethod;

    static {
        try {
            lanuchMethod = WebSocketClientLauncher.class.getMethod("launchWSClient",
                    Object.class, Object.class, Object.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 线程池
     */
    private final ExecutorService executorService;

    /**
     * bot 对象
     */
    private final AnnoDriveAutoBot<?> bot;

    /**
     * 控制并发数量的信号量
     */
    private final Map<String, Semaphore> wsCCSemapthoreMap = new ConcurrentHashMap<>();

    /**
     * 已启动的ws客户端
     */
    private final Map<String, BaseBotWSClient<?, ?>> launchedWSClientMap = new ConcurrentHashMap<>();

    public WebSocketClientLauncher(
            AnnoDriveAutoBot<?> bot
    ) {
        this.bot = bot;
        this.executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory("ws-client-launcher"));
    }

    /**
     * 构建ws客户端
     *
     * @param accountContextObj accountContextObj
     * @param jobParamObj       jobParamObj
     * @return BaseBotWSClient < Req, Resp>
     */
    public final CompletableFuture<Result> launchWSClient(
            Object accountContextObj,
            Object jobParamObj,
            Object wsClientBuilderObj
    ) {
        // 参数处理
        AccountContext accountContext = (AccountContext) accountContextObj;
        AutoBotJobParam jobParam = (AutoBotJobParam) jobParamObj;
        AccountWSClientBuilder wsClientBuilder = (AccountWSClientBuilder) wsClientBuilderObj;

        String key = generateAccountKey(accountContext, jobParam.getJobName());

        String prefix = String.format("bot[%s]-job[%s]-account[%s]-[%s]",
                jobParam.getGroup(), jobParam.getJobName(), accountContext.getId(), accountContext.getName());

        BotWSMethodConfig botWSMethodConfig = jobParam.getBotWSMethodConfig();


        // 已经启动的，不再创建
        if (launchedWSClientMap.containsKey(key)) {
            if (!botWSMethodConfig.isRefreshWSConnection()) {
                log.warn("{} ws client already created", prefix);
                return CompletableFuture.completedFuture(Result.fail(prefix + "ws client already created"));
            } else {
                log.warn(prefix + " 移除旧ws客户端");
                launchedWSClientMap.remove(key).shutdown();
            }
        }

        return buildAndConnectWebSocket(accountContext, botWSMethodConfig, key, wsClientBuilder, prefix);
    }


    /**
     * 构建并连接ws客户端
     *
     * @param accountContext    accountContext
     * @param botWSMethodConfig botWSMethodConfig
     * @param key               key
     * @param wsClientBuilder   wsClientBuilder
     * @param prefix            prefix
     * @return CompletableFuture<Result>
     */
    private @NotNull CompletableFuture<Result> buildAndConnectWebSocket(
            AccountContext accountContext,
            BotWSMethodConfig botWSMethodConfig,
            String key,
            AccountWSClientBuilder wsClientBuilder,
            String prefix
    ) {

        // Step 1 ws 连接数量控制
        Semaphore wsConnectSemaphore;
        try {
            wsConnectSemaphore = wsCCSemapthoreMap.computeIfAbsent(key, k -> new Semaphore(botWSMethodConfig.wsConnectCount()));
            wsConnectSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //  Step 2 启动
        BaseBotWSClient<?, ?> wsClient = null;

        // Step 3 创建ws client
        try {
            wsClient = wsClientBuilder.build(accountContext);

            launchedWSClientMap.put(key, wsClient);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("build account ws client error", e);
            return CompletableFuture.completedFuture(Result.fail(prefix + "build account ws client error"));
        }

        // Step 4 设置handler
        init(wsClient, botWSMethodConfig, wsConnectSemaphore);

        // Step 5 检查当前状态，如果为new或stop才进行启动
        WebsocketClientStatus currentStatus = wsClient.getClientStatus();


        return switch (currentStatus) {
            // 新创建，停止状态，需要建立连接
            case NEW, STOP -> wsClient
                    .connect() // 异步连接
                    .thenApplyAsync(connectResult -> {
                        if (connectResult) {
                            // 连接成功
                            return Result.ok(prefix + "connect success");
                        } else if (botWSMethodConfig.wsUnlimitedRetry()) {
                            // TODO ws需要加入shutdown后的恢复机制
                            // 连接失败，且允许无限重连, 返回ok，ws内部会自动重连
                            return Result.ok(prefix + "connect fail, restarting...");
                        } else {
                            return Result.fail(prefix + "ws client can not connect");
                        }
                    }, executorService)
                    .exceptionallyAsync(throwable -> {
                        log.error("ws client connect error", throwable);
                        return Result.fail(prefix + "connect error, " + throwable.getMessage());
                    }, executorService);
            case STARTING, RUNNING -> CompletableFuture.completedFuture(Result.ok());
            // 被禁止使用，抛出异常
            case SHUTDOWN -> CompletableFuture.completedFuture(Result.fail(prefix + " ws client can not connect"));
        };
    }

    /**
     * 生成账户key
     *
     * @param accountContext accountContext
     * @param jobName        jobName
     * @return String
     */
    private static @NotNull String generateAccountKey(AccountContext accountContext, String jobName) {
        return jobName + accountContext.getName();
    }


    /**
     * 添加ws状态改变的handler
     *
     * @param <Req>             Req
     * @param <Resp>            Resp
     * @param wsClient          wsClient
     * @param botWSMethodConfig botWSMethodConfig
     */
    private <Req, Resp> void init(BaseBotWSClient<Req, Resp> wsClient, BotWSMethodConfig botWSMethodConfig, Semaphore wsConnectSemaphore) {
        // 设置参数
        wsClient.setAllIdleTimeSecond(botWSMethodConfig.heartBeatIntervalSecond());
        wsClient.setReconnectCountDownSecond(botWSMethodConfig.reconnectCountDownSecond());
        wsClient.setReconnectLimit(botWSMethodConfig.reconnectLimit());

        //设置相关回调
        wsClient.setClientStatusChangeHandler(newStatus -> {
            wsClient.whenClientStatusChange(newStatus);
            // 释放资源
            if (newStatus.equals(WebsocketClientStatus.SHUTDOWN)) {
                // 记录完成的ws数量
                wsConnectSemaphore.release();
            }
        });
    }
}
