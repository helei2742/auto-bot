package cn.com.helei.DepinBot.core.pool.account;


import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepinClientAccount {

    /**
     * 账户名
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码
     */
    private String password;


    /**
     * 代理id
     */
    private Integer proxyId;


    /**
     * 浏览器环境id
     */
    private Integer browserEnvId;


    public DepinClientAccount(String emailAndPassword) {
        String[] split = emailAndPassword.split(", ");
        email = split[0];
        password = split[1];
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
