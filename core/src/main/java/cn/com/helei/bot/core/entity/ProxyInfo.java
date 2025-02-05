package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.constants.ProxyProtocol;
import cn.com.helei.bot.core.constants.ProxyType;
import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTYpeHandler;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;

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
@TableName("t_proxy_info")
@AllArgsConstructor
@NoArgsConstructor
public class ProxyInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "proxy_type")
    private ProxyType proxyType;

    @TableField(value = "proxy_protocol")
    private ProxyProtocol proxyProtocol;

    @TableField("host")
    private String host;

    @TableField("port")
    private Integer port;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    private volatile boolean usable = true;

    private JSONObject metadata;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTYpeHandler.class)
    private LocalDateTime updateDatetime;

    @TableField("is_valid")
    private Integer isValid;

    public ProxyInfo(Object originLine) {
        String proxyUrl = (String) originLine;

        String[] split = proxyUrl.split("://");
        String protocol = split[0];

        proxyProtocol = switch (protocol) {
            case "http" -> ProxyProtocol.HTTP;
            case "sockt5" -> ProxyProtocol.SOCKT5;
            default -> throw new IllegalStateException("Unexpected value: " + protocol);
        };
        String[] upAndAddress = split[1].split("@");

        if (upAndAddress.length == 1) {
            String[] address = upAndAddress[0].split(":");
            this.host = address[0];
            this.port = Integer.parseInt(address[1]);
        } else if (upAndAddress.length == 2) {
            String[] up = upAndAddress[0].split(":");
            this.username = up[0];
            this.password = up[1];

            String[] address = upAndAddress[1].split(":");
            this.host = address[0];
            this.port = Integer.parseInt(address[1]);
        }
    }


    public SocketAddress getAddress() {
        return new InetSocketAddress(host, port);
    }

    public String getAddressStr() {
        return host + ":" + port;
    }
}
