package cn.com.helei.DepinBot.core.dto;


import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class DepinClientAccount {

    /**
     * 账户名
     */
    private String name;

    /**
     * 代理id
     */
    private Integer proxyId;


    /**
     * 浏览器环境id
     */
    private Integer browserEnvId;


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
