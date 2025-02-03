package cn.com.helei.bot.core.dto.account;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.pool.twitter.Twitter;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import cn.com.helei.bot.core.pool.env.BrowserEnv;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.com.helei.bot.core.dto.RewordInfo;
import com.alibaba.fastjson.annotation.JSONField;
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
    private AccountBaseInfo accountBaseInfo;

    /**
     * 账户对应的twitter
     */
    private Twitter twitter;

    /**
     * 代理
     */
    private NetworkProxy proxy;

    /**
     * 浏览器环境
     */
    private BrowserEnv browserEnv;

    /**
     * 连接的url
     */
    private String connectUrl;

    /**
     * 连接状态
     */
    private final ConnectStatusInfo connectStatusInfo = new ConnectStatusInfo();

    /**
     * 分数信息
     */
    @PropertyChangeListenField
    private RewordInfo rewordInfo = new RewordInfo();

    @PropertyChangeListenField
    @JSONField(serialize = true, deserialize = true)
    private Map<String, String> params = new HashMap<>();


    private LocalDateTime saveDatetime;


    public String getParam(String key) {
        return params.get(key);
    }

    public void setParam(String key, String value) {
        params.put(key, value);
    }

    public Map<String, String> getWSHeaders() {
        return accountBaseInfo.getWSHeaders();
    }

    public Map<String, String> getRestHeaders() {
        return accountBaseInfo.getRestHeaders();
    }

    public String getName() {
        return accountBaseInfo.getName() == null ? accountBaseInfo.getEmail() : accountBaseInfo.getName();
    }

    public String getSimpleInfo() {
        return String.format("%s-账户[%s]-代理[%s]", getAccountBaseInfo().getId(), getName(), getProxy() == null ? "NO_PROXY" : getProxy().getAddressStr());
    }

    public void removeParam(String key) {
        params.remove(key);
    }
}
