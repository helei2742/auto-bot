package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.bot.core.util.excel.IntegerStringConverter;
import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.helei.bot.core.util.typehandler.MapTextTypeHandler;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;

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
@TableName("t_bot_account_context")
@NoArgsConstructor
@Builder
@AllArgsConstructor
@PropertyChangeListenClass(isDeep = true)
public class AccountContext {


    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("bot_id")
    private Integer botId;

    @TableField("bot_key")
    @ExcelProperty(value = "bot_key")
    private String botKey;

    @TableField("account_base_info_id")
    @ExcelProperty(value = "account_base_info_id", converter = IntegerStringConverter.class)
    private Integer accountBaseInfoId;

    @TableField("twitter_id")
    @ExcelProperty(value = "twitter_id", converter = IntegerStringConverter.class)
    private Integer twitterId;

    @TableField("discord_id")
    @ExcelProperty(value = "discord_id", converter = IntegerStringConverter.class)
    private Integer discordId;

    @TableField("proxy_id")
    @ExcelProperty(value = "proxy_id", converter = IntegerStringConverter.class)
    private Integer proxyId;

    @TableField("browser_env_id")
    @ExcelProperty(value = "browser_env_id", converter = IntegerStringConverter.class)
    private Integer browserEnvId;

    @TableField("telegram_id")
    @ExcelProperty(value = "telegram_id", converter = IntegerStringConverter.class)
    private Integer telegramId;

    @TableField("wallet_id")
    @ExcelProperty(value = "wallet_id", converter = IntegerStringConverter.class)
    private Integer walletId;

    @TableField("reward_id")
    private Integer rewardId;

    /**
     * 账号状态
     * 0 表示初始状态
     * 1 表示已注册
     */
    @TableField("status")
    @PropertyChangeListenField
    private Integer status;

    @JSONField(serialize = true, deserialize = true)
    @TableField(value = "params", typeHandler = MapTextTypeHandler.class)
    @PropertyChangeListenField
    private Map<String, Object> params = new HashMap<>();

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;


    @TableField(exist = false)
    @PropertyChangeListenField
    private RewordInfo rewordInfo = new RewordInfo();

    @TableField(exist = false)
    private AccountBaseInfo accountBaseInfo;

    @TableField(exist = false)
    private TwitterAccount twitter;

    @TableField(exist = false)
    private DiscordAccount discord;

    @TableField(exist = false)
    private TelegramAccount telegram;

    @TableField(exist = false)
    private ProxyInfo proxy;

    @TableField(exist = false)
    private BrowserEnv browserEnv;

    @TableField(exist = false)
    private final ConnectStatusInfo connectStatusInfo = new ConnectStatusInfo();

    public String getParam(String key) {
        return String.valueOf(params.get(key));
    }

    public void setParam(String key, Object value) {
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

    public Boolean isSignUp() {
        return status != null && status == 1;
    }

    public String getType() {
        return accountBaseInfo == null ? null : accountBaseInfo.getType();
    }

    public static void signUpSuccess(AccountContext accountContext) {
        accountContext.setStatus(1);
    }
}
