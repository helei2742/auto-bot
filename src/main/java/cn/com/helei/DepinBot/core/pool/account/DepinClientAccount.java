package cn.com.helei.DepinBot.core.pool.account;


import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenClass;
import cn.com.helei.DepinBot.core.supporter.propertylisten.PropertyChangeListenField;
import cn.com.helei.DepinBot.core.pool.AbstractYamlLineItem;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.*;


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
     * 代理id
     */
    @PropertyChangeListenField
    private Integer proxyId;


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
            this.browserEnvId = Integer.valueOf(split[2]);
        }
    }

    public HttpHeaders getWSHeaders() {
        return new DefaultHttpHeaders();
    }

    public HttpHeaders getRestHeaders() {
        return new DefaultHttpHeaders();
    }

    public String getConnectUrl() {
        return "";
    }
}
