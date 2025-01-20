package cn.com.helei.DepinBot.core.dto;

import cn.com.helei.DepinBot.core.env.BrowserEnv;
import cn.com.helei.DepinBot.core.network.NetworkProxy;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountContext {

    /**
     * 账户是否可用
     */
    private boolean usable = true;

    /**
     * client 账户
     */
    private DepinClientAccount clientAccount;

    /**
     * 代理
     */
    private NetworkProxy proxy;

    /**
     * 浏览器环境
     */
    private BrowserEnv browserEnv;

    /**
     * 连接状态
     */
    private final ConnectStatusInfo connectStatusInfo = new ConnectStatusInfo();

    /**
     * 分数信息
     */
    private final RewordInfo rewordInfo = new RewordInfo();

    public HttpHeaders getWSHeaders() {
        return clientAccount.getWSHeaders();
    }

    public HttpHeaders getRestHeaders() {

        return clientAccount.getRestHeaders();
    }
}
