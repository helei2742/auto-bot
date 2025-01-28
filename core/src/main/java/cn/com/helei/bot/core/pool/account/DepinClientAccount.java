package cn.com.helei.bot.core.pool.account;


import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.bot.core.supporter.propertylisten.PropertyChangeListenField;
import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
import lombok.*;

import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@PropertyChangeListenClass
public class DepinClientAccount extends AbstractYamlLineItem {

    /**
     * 账户名
     */
    @PropertyChangeListenField
    private String name;

    /**
     * 邮箱
     */
    @PropertyChangeListenField
    private String email;

    /**
     * 密码
     */
    @PropertyChangeListenField
    private String password;

    /**
     * 是否注册过
     */
    @PropertyChangeListenField
    private Boolean signUp;

    /**
     * 代理id
     */
    @PropertyChangeListenField
    private Integer proxyId;

    /**
     * 推特id
     */
    @PropertyChangeListenField
    private Integer twitterId;

    /**
     * 浏览器环境id
     */
    @PropertyChangeListenField
    private Integer browserEnvId;


    public DepinClientAccount(Object originLine) {
        String emailAndPassword = (String) originLine;

        String[] split = emailAndPassword.split(", ");
        email = split[0];

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            throw new IllegalArgumentException("邮箱格式错误");
        }
        this.name = emailSplit[0];


        password = split[1];

        if (split.length == 3) {
            this.proxyId = Integer.valueOf(split[2]);
        }
        if (split.length == 4) {
            this.browserEnvId = Integer.valueOf(split[3]);
        }
        if (split.length == 5) {
            this.twitterId = Integer.valueOf(split[4]);
        }
    }

    public Map<String, String> getWSHeaders() {
        return new HashMap<>();
    }

    public Map<String, String> getRestHeaders() {
        return new HashMap<>();
    }

}
