package cn.com.helei.DepinBot.core.network;

import lombok.Data;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Data
public class NetworkProxy {
    private Integer id;

    private String host;

    private int port;

    private String username;

    private String password;

    public SocketAddress getAddress() {
        return new InetSocketAddress(host, port);
    }
}
