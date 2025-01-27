package cn.com.helei.DepinBot;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.codec.http.websocketx.*;
import java.net.URI;

public class WebSocketClientDemo {

    public static void main(String[] args) throws Exception {
        // WebSocket URL
        String url = "wss://game.keitokun.com/api/v1/ws?uid=74923709039";
        URI uri = new URI(url);

        // 主机名和端口号
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 443 : uri.getPort();

        // SSL 上下文配置
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // 仅用于测试，生产环境请使用有效证书
                .build();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 添加 SSL 处理器
                            pipeline.addLast(sslContext.newHandler(ch.alloc(), host, port));
                            // HTTP 编解码器
                            pipeline.addLast(new HttpClientCodec());
                            // HTTP 消息聚合器
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            // WebSocket 协议处理器
                            pipeline.addLast(new WebSocketClientProtocolHandler(
                                    uri, // WebSocket URI
                                    WebSocketVersion.V13, // 使用的 WebSocket 协议版本
                                    null, // 子协议（如果不需要子协议，可以为 null）
                                    true, // 是否允许扩展
                                    null, // 自定义 HTTP Headers（如果没有，可以为 null）
                                    65536 // 最大帧长度
                            ));
                            // 自定义消息处理器
                            pipeline.addLast(new WebSocketClientHandler());
                        }
                    });

            // 连接服务器
            Channel channel = bootstrap.connect(host, port).sync().channel();

            // 等待连接关闭
            channel.closeFuture().sync();
        } finally {
            // 关闭线程组
            group.shutdownGracefully();
        }
    }

    static class WebSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("WebSocket 连接已建立！");
            // 发送测试消息
            ctx.writeAndFlush(new TextWebSocketFrame("Hello, WebSocket!"));
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
            if (frame instanceof TextWebSocketFrame) {
                System.out.println("收到消息: " + ((TextWebSocketFrame) frame).text());
            } else if (frame instanceof CloseWebSocketFrame) {
                System.out.println("WebSocket 连接关闭: " + frame);
                ctx.channel().close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("发生异常: " + cause.getMessage());
            ctx.close();
        }
    }
}
