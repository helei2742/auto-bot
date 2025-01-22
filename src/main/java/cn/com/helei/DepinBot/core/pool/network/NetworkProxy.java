package cn.com.helei.DepinBot.core.pool.network;

import cn.com.helei.DepinBot.core.pool.AbstractYamlLineItem;
import lombok.*;

        import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Getter
@Setter
@NoArgsConstructor
public class NetworkProxy extends AbstractYamlLineItem {

    private ProxyType proxyType;

    private String host;

    private int port;

    private String username;

    private String password;

    public NetworkProxy(Object originLine) {
        String proxyUrl = (String) originLine;

        String[] split = proxyUrl.split("://");
        String protocol = split[0];

        proxyType = switch (protocol) {
            case "http" -> ProxyType.HTTP;
            case "sockt5" -> ProxyType.SOCKT5;
            default -> throw new IllegalStateException("Unexpected value: " + protocol);
        };
        String[] upAndAddress = split[1].split("@");

        String[] up = upAndAddress[0].split(":");
        this.username = up[0];
        this.password = up[1];

        String[] address = upAndAddress[1].split(":");
        this.host = address[0];
        this.port = Integer.parseInt(address[1]);

    }

    public SocketAddress getAddress() {
        return new InetSocketAddress(host, port);
    }
}
