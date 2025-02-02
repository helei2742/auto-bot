package cn.com.helei.bot.core.netty.handler;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.Setter;

import java.net.URI;
import java.util.Map;

public class CustomHeaderWSClientHandshaker extends WebSocketClientHandshaker13 {

    @Setter
    private Map<String, String> headers;

    public CustomHeaderWSClientHandshaker(URI webSocketURL, WebSocketVersion version, String subprotocol, boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength) {
        super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);
    }

    @Override
    protected FullHttpRequest newHandshakeRequest() {
        FullHttpRequest request = super.newHandshakeRequest();
        if (headers != null) {
            headers.forEach((key, value) -> request.headers().add(key, value));
        }
        return request;
    }
}
