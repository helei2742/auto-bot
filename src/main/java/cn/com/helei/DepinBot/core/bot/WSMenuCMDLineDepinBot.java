package cn.com.helei.DepinBot.core.bot;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.exception.DepinBotStatusException;
import cn.com.helei.DepinBot.core.netty.constants.WebsocketClientStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
public abstract class WSMenuCMDLineDepinBot<C extends BaseDepinBotConfig, Req, Resp> extends DefaultMenuCMDLineDepinBot<C> {

    private final Map<AccountContext, BaseDepinWSClient<Req, Resp>> accountWSClientMap;

    public WSMenuCMDLineDepinBot(C config) {
        super(config);
        accountWSClientMap = new ConcurrentHashMap<>();
    }

    @Override
    protected boolean doAccountClaim(AccountContext accountContext) {
        BaseDepinWSClient<Req, Resp> depinWSClient = accountWSClientMap.compute(accountContext, (k, v) -> {
            // 没有创建过，或被关闭，创建新的
            if (v == null || v.getClientStatus().equals(WebsocketClientStatus.SHUTDOWN)) {
                v = buildAccountWSClient(accountContext);
            }

            return v;
        });

        String accountName = accountContext.getClientAccount().getName();

        //Step 3 建立连接
        WebsocketClientStatus clientStatus = depinWSClient.getClientStatus();
        return switch (clientStatus) {
            case NEW, STOP:  // 新创建，停止状态，需要建立连接
                try {
                    yield depinWSClient
                            .connect()
                            .thenApplyAsync(success -> {
                                try {
                                    whenAccountConnected(depinWSClient, success);
                                } catch (Exception e) {
                                    log.error("账户[{}]-连接完成后执行回调发生错误", accountName, e);
                                    return true;
                                }
                                return false;
                            }, getExecutorService()).get();
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


}
