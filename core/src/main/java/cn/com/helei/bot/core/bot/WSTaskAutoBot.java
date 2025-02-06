package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.config.AutoBotConfig;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.util.exception.DepinBotStatusException;
import cn.com.helei.bot.core.supporter.netty.constants.WebsocketClientStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

@Slf4j
public abstract class WSTaskAutoBot<C extends AutoBotConfig, Req, Resp> extends RestTaskAutoBot {

    /**
     * WS链接完成数量
     */
    protected static final String WS_COMPLETE_COUNT_KEY = "ws_complete_count";

    /**
     * 账户ws创建数量
     */
    protected static final String WS_CREATE_COUNT_KEY = "ws_create_count";

    /**
     * AccountContext map BaseDepinWSClient
     */
    private final Map<AccountContext, BaseBotWSClient<Req, Resp>> accountWSClientMap;

    /**
     * 控制并发数量的信号量
     */
    private final Semaphore wsConnectSemaphore;

    @Getter
    private final C botConfig;

    public WSTaskAutoBot(C config, BotApi botApi) {
        super(config, botApi);

        this.botConfig = config;
        this.wsConnectSemaphore = new Semaphore(config.getWebsocket().getWsConnectCount());
        this.accountWSClientMap = new ConcurrentHashMap<>();
    }

    /**
     * 开始账户claim
     */
    @Override
    protected final void doAccountsClaim(String type, List<AccountContext> accountContexts) {
        accountContexts.forEach(account -> {
            account.getConnectStatusInfo().setStartDateTime(LocalDateTime.now());
            getExecutorService().execute(() -> doAccountClaim(account));
        });
    }


    @Override
    public final boolean doAccountClaim(AccountContext accountContext) {
        // Step 1 获取锁，才能进行claim
        try {
            wsConnectSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Step 2 创建或获取账户对应的depinWSClient
        BaseBotWSClient<Req, Resp> depinWSClient = accountWSClientMap.compute(accountContext, (k, v) -> {
            // 没有创建过，或被关闭，创建新的
            if (v == null || v.getClientStatus().equals(WebsocketClientStatus.SHUTDOWN)) {
                v = buildAccountWSClient(accountContext);

                if (v != null) {
                    v.setReconnectCountDownSecond(getBotConfig().getWebsocket().getReconnectCountDownSecond());
                    v.setAllIdleTimeSecond(getBotConfig().getWebsocket().getHeartBeatIntervalSecond());

                    // 记录创建的ws数量
                    addWSRuntimeInfoCount(WS_CREATE_COUNT_KEY);
                }
            }

            return v;
        });

        // Step 3 获取到null，释放资源返回失败
        if (depinWSClient == null) {
            wsConnectSemaphore.release();
            return false;
        }

        String accountName = accountContext.getAccountBaseInfo().getName();

        //Step 4 设置相关回调
        WebsocketClientStatus currentStatus = depinWSClient.getClientStatus();

        depinWSClient.setClientStatusChangeHandler(newStatus -> {
            depinWSClient.whenClientStatusChange(newStatus);

            // 调用bot类的回调方法
            whenAccountClientStatusChange(depinWSClient, newStatus);

            // 释放资源
            if (newStatus.equals(WebsocketClientStatus.SHUTDOWN)) {

                // 记录完成的ws数量
                addWSRuntimeInfoCount(WS_COMPLETE_COUNT_KEY);

                wsConnectSemaphore.release();
            }
        });

        // Step 5 检查当前状态，如果为new 或stop才进行启动
        return switch (currentStatus) {
            case NEW, STOP:  // 新创建，停止状态，需要建立连接
                try {
                    yield !depinWSClient.connect().get() && getBotConfig().getWebsocket().isWsUnlimitedRetry();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("账户[{}]ws链接发生错误", accountName, e);
                    yield true;
                }
            case STARTING, RUNNING:
                yield false;
            case SHUTDOWN: // 被禁止使用，抛出异常
                throw new DepinBotStatusException("cannot start ws client when it shutdown, " + accountName);
        };
    }


    /**
     * 使用accountContext构建AbstractDepinWSClient
     *
     * @param accountContext accountContext
     * @return AbstractDepinWSClient
     */
    public abstract BaseBotWSClient<Req, Resp> buildAccountWSClient(AccountContext accountContext);


    /**
     * 当账户链接时调用
     *
     * @param depinWSClient depinWSClient
     * @param clientStatus  clientStatus
     */
    public abstract void whenAccountClientStatusChange(BaseBotWSClient<Req, Resp> depinWSClient, WebsocketClientStatus clientStatus);

    /**
     * 当ws连接收到响应
     *
     * @param depinWSClient depinWSClient
     * @param id            id
     * @param response      response
     */
    public abstract void whenAccountReceiveResponse(BaseBotWSClient<Req, Resp> depinWSClient, Object id, Resp response);

    /**
     * 当ws连接收到消息
     *
     * @param depinWSClient depinWSClient
     * @param message       message
     */
    public abstract void whenAccountReceiveMessage(BaseBotWSClient<Req, Resp> depinWSClient, Resp message);


    /**
     * 获取心跳消息
     *
     * @param depinWSClient depinWSClient
     * @return 消息体
     */
    public abstract Req getHeartbeatMessage(BaseBotWSClient<Req, Resp> depinWSClient);


    /**
     * 给key的DepinBotRuntimeInfo加1
     *
     * @param key key
     */
    protected void addWSRuntimeInfoCount(String key) {
        getAutoBotRuntimeInfo().getKeyValueInfoMap().compute(key, (k1, v1) -> {
            if (v1 == null) {
                v1 = 0;
            }
            return ((int) v1) + 1;
        });
    }
}
