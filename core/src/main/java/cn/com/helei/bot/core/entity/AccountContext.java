package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTYpeHandler;
import cn.com.helei.bot.core.util.typehandler.MapTextTypeHandler;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.*;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("t_project_account_context")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AccountContext {


    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("account_base_info_id")
    private Integer accountBaseInfoId;

    private AccountBaseInfo accountBaseInfo;

    @TableField("reward_id")
    private Integer rewardId;

    @PropertyChangeListenField
    private RewordInfo rewordInfo = new RewordInfo();

    @TableField("twitter_id")
    private Integer twitterId;

    private TwitterAccount twitter;

    @TableField("discord_id")
    private Integer discordId;

    private DiscordAccount discord;

    @TableField("proxy_id")
    private Integer proxyId;

    private ProxyInfo proxy;

    @TableField("browser_env_id")
    private Integer browserEnvId;

    private BrowserEnv browserEnv;

    @TableField("telegram_id")
    private Integer telegramId;

    @TableField("wallet_id")
    private Integer walletId;

    @PropertyChangeListenField
    @TableField("status")
    private Integer status;

    @PropertyChangeListenField
    @TableField("usable")
    private boolean usable = true;

    @PropertyChangeListenField
    @JSONField(serialize = true, deserialize = true)
    @TableField(value = "params", typeHandler = MapTextTypeHandler.class)
    private Map<String, String> params = new HashMap<>();

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime updateDatetime;

    @TableField("is_valid")
    private Integer isValid;

    private final ConnectStatusInfo connectStatusInfo = new ConnectStatusInfo();

    public String getParam(String key) {
        return params.get(key);
    }

    public void setParam(String key, String value) {
        params.put(key, value);
    }

    public void removeParam(String key) {
        params.remove(key);
    }

    public String getName() {
        return accountBaseInfo.getName() == null ? accountBaseInfo.getEmail() : accountBaseInfo.getName();
    }

    public String getSimpleInfo() {
        return String.format("%s-账户[%s]-代理[%s]", getAccountBaseInfo().getId(), getName(), getProxy() == null ? "NO_PROXY" : getProxy().getAddressStr());
    }

    public Boolean getSignUp() {
        return status == 1;
    }

    public void setSignUp(boolean b) {
        status = 1;
    }

    public String getType() {
        return accountBaseInfo == null ? null : accountBaseInfo.getType();
    }
}
