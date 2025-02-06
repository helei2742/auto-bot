package cn.com.helei.bot.core.entity;

import cn.com.helei.bot.core.constants.ProxyProtocol;
import cn.com.helei.bot.core.constants.ProxyType;
import cn.com.helei.bot.core.util.excel.IntegerStringConverter;
import cn.com.helei.bot.core.util.excel.ProxyProtocolConverter;
import cn.com.helei.bot.core.util.typehandler.LocalDateTimeTypeHandler;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.Map;

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
@Builder
public class ProxyInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "proxy_type")
    private ProxyType proxyType;

    @TableField(value = "proxy_protocol")
    @ExcelProperty(value = "proxy_protocol", converter = ProxyProtocolConverter.class)
    private ProxyProtocol proxyProtocol;

    @TableField("host")
    @ExcelProperty(value = "host")
    private String host;

    @TableField("port")
    @ExcelProperty(value = "port", converter = IntegerStringConverter.class)
    private Integer port;

    @TableField("username")
    @ExcelProperty(value = "username")
    private String username;

    @TableField("password")
    @ExcelProperty(value = "password")
    private String password;

    @TableField("params")
    private Map<String, Object> params;

    private volatile boolean usable = true;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;


    public ProxyInfo(Object originLine) {
        String proxyUrl = (String) originLine;

        String[] split = proxyUrl.split("://");
        String protocol = split[0];

        proxyProtocol = switch (protocol.toLowerCase()) {
            case "http" -> ProxyProtocol.HTTP;
            case "socks5" -> ProxyProtocol.SOCKS5;
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
