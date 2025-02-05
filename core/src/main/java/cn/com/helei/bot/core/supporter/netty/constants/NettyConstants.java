package cn.com.helei.bot.core.supporter.netty.constants;

import io.netty.util.AttributeKey;

public class NettyConstants {

    /**
     * 一朕的最大长度
     */
    public final static int MAX_FRAME_LENGTH = 1024;


    /**
     * 放在netty channel 里的 client_name 的 key
     */
    public static final AttributeKey<String> CLIENT_NAME = AttributeKey.valueOf("client_name");

    /**
     * 请求等待时间
     */
    public static final long REQUEST_WAITE_SECONDS = 60;

    /**
     * netty客户端断线重连时间
     */
    public static final int RECONNECT_DELAY_SECONDS = 5;


    /**
     * netty客户端断线重连次数
     */
    public static final int RECONNECT_LIMIT = 3;
}
