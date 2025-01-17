package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.constants.ConnectStatus;
import cn.com.helei.DepinBot.core.netty.base.AbstractWebsocketClient;
import cn.com.helei.DepinBot.core.netty.constants.WebsocketClientStatus;
import cn.com.helei.DepinBot.core.util.RestApiClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Slf4j
@Getter
public class AbstractDepinWSClient<Req, Resp> extends AbstractWebsocketClient<Req, Resp> {

    /**
     * 网络请求客户端
     */
    private final RestApiClient restApiClient;

    /**
     * client对应的账号
     */
    private final AccountContext accountContext;

    public AbstractDepinWSClient(
            AccountContext accountContext,
            AbstractDepinWSClientHandler<Req, Resp> handler
    ) {
        super(accountContext.getClientAccount().getConnectUrl(), handler);
        super.setName(accountContext.getClientAccount().getName());
        super.setHeaders(accountContext.getHeaders());

        super.setProxy(accountContext.getProxy());

        super.setClientStatusChangeHandler(this::whenClientStatusChange);

        this.accountContext = accountContext;
        this.restApiClient = new RestApiClient(accountContext.getProxy(), super.getCallbackInvoker());

        updateClientStatus(WebsocketClientStatus.NEW);
    }

    /**
     * ws客户端状态改变，同步更新账户状态
     *
     * @param newClientStatus 最新的客户端状态
     */
    public void whenClientStatusChange(WebsocketClientStatus newClientStatus) {
        accountContext.getConnectStatusInfo().setConnectStatus(
                switch (newClientStatus) {
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
                        accountContext.setUsable(false);
                        yield ConnectStatus.STOP;
                    }
                }
        );
    }
}
