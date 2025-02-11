package cn.com.helei.bot.core.supporter.netty;

import cn.com.helei.bot.core.constants.ConnectStatus;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.netty.base.AbstractWebsocketClient;
import cn.com.helei.bot.core.supporter.netty.constants.WebsocketClientStatus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Slf4j
@Getter
public abstract class BaseBotWSClient<Req, Resp> extends AbstractWebsocketClient<Req, Resp> {

    /**
     * client对应的账号
     */
    private final AccountContext accountContext;

    public BaseBotWSClient(
            AccountContext accountContext,
            String connectUrl,
            BaseBotWSClientHandler<Req, Resp> handler
    ) {
        super(connectUrl, handler);

        super.setName(accountContext.getName());
        super.setProxy(accountContext.getProxy());
        super.setClientStatusChangeHandler(this::whenClientStatusChange);

        this.accountContext = accountContext;

        updateClientStatus(WebsocketClientStatus.NEW);
    }


    public abstract Req getHeartbeatMessage();

    public abstract void whenAccountReceiveResponse(Object id, Resp response) ;

    public abstract void whenAccountReceiveMessage(Resp message);

    public abstract Object getRequestId(Req request);

    public abstract Object getResponseId(Resp response);

    public void whenAccountClientStatusChange(WebsocketClientStatus clientStatus) {}

    /**
     * ws客户端状态改变，同步更新账户状态
     *
     * @param newClientStatus 最新的客户端状态
     */
    public void whenClientStatusChange(WebsocketClientStatus newClientStatus) {
        ConnectStatus connectStatus = switch (newClientStatus) {
            case NEW -> {
                accountContext.getConnectStatusInfo().setStartDateTime(LocalDateTime.now());
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.NEW;
            }
            case STARTING -> {
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.STARTING;
            }
            case RUNNING -> {
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.RUNNING;
            }
            case STOP, SHUTDOWN -> {
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.STOP;
            }
        };

        accountContext.getConnectStatusInfo().setConnectStatus(connectStatus);

        whenAccountClientStatusChange(newClientStatus);
    }

}
