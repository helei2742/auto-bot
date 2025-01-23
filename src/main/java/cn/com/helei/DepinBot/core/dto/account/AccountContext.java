package cn.com.helei.DepinBot.core.dto.account;

import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenField;
import cn.com.helei.DepinBot.core.pool.account.DepinClientAccount;
import cn.com.helei.DepinBot.core.pool.env.BrowserEnv;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import cn.com.helei.DepinBot.core.dto.RewordInfo;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@PropertyChangeListenClass(isDeep = true)
public class AccountContext {

    /**
     * 账户是否可用
     */
    @PropertyChangeListenField
    private boolean usable = true;

    /**
     * client 账户
     */
    @PropertyChangeListenField
    private DepinClientAccount clientAccount;

    /**
     * 代理
     */
    private NetworkProxy proxy;

    /**
     * 浏览器环境
     */
    private BrowserEnv browserEnv;

    private LocalDateTime saveDatetime;


    /**
     * 连接状态
     */
    private final ConnectStatusInfo connectStatusInfo = new ConnectStatusInfo();

    /**
     * 分数信息
     */
    @PropertyChangeListenField
    private final RewordInfo rewordInfo = new RewordInfo();

    @PropertyChangeListenField
    private final Map<String, String> params = new HashMap<>();

    public String getParam(String key) {
        return params.get(key);
    }

    public void setParam(String key, String value) {
        params.put(key, value);
    }

    public Map<String, String> getWSHeaders() {
        return clientAccount.getWSHeaders();
    }

    public Map<String, String> getRestHeaders() {
        return clientAccount.getRestHeaders();
    }

    public String getName() {
        return clientAccount.getName() == null ? clientAccount.getEmail() : clientAccount.getName();
    }
}
