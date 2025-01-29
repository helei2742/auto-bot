package cn.com.helei.bot.core.pool.network;

import cn.com.helei.bot.core.pool.AbstractYamlLineItem;
import lombok.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Getter
@Setter
@NoArgsConstructor
public class NetworkProxy extends AbstractYamlLineItem {

    private ProxyType proxyType;

    private ProxyProtocal proxyProtocal;

    private String host;

    private int port;

    private String username;

    private String password;

    public NetworkProxy(Object originLine) {
        String proxyUrl = (String) originLine;

        String[] split = proxyUrl.split("://");
        String protocol = split[0];

        proxyProtocal = switch (protocol) {
            case "http" -> ProxyProtocal.HTTP;
            case "sockt5" -> ProxyProtocal.SOCKT5;
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
