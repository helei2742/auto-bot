package cn.com.helei.DepinBot.core.netty.base;


import cn.com.helei.DepinBot.core.netty.handler.WSCloseHandler;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import cn.com.helei.DepinBot.core.netty.constants.NettyConstants;
import cn.com.helei.DepinBot.core.netty.constants.WebsocketClientStatus;
import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Websocket客户端
 *
 * @param <P> 请求体的类型
 * @param <T> 返回值的类型
 */
@Slf4j
public abstract class AbstractWebsocketClient<P, T> {

    private static final int MAX_FRAME_SIZE = 10 * 1024 * 1024;  // 10 MB or set to your desired size

    /**
     * websocket的url字符串
     */
    protected String url;

    /**
     * netty pipeline 最后一个执行的handler
     */
    protected final AbstractWebSocketClientHandler<P, T> handler;

    /**
     * 执行回调的线程池
     */
    @Getter
    protected final ExecutorService callbackInvoker;

    /**
     * 代理
     */
    @Setter
    protected NetworkProxy proxy = null;

    @Setter
    protected HttpHeaders headers;

    /**
     * 空闲时间
     */
    @Setter
    protected int allIdleTimeSecond = 10;

    /**
     * 重链接次数
     */
    private final AtomicInteger reconnectTimes = new AtomicInteger(0);

    /**
     * 重连锁
     */
    private final ReentrantLock reconnectLock = new ReentrantLock();

    /**
     * 启动中阻塞的condition
     */
    private final Condition startingWaitCondition = reconnectLock.newCondition();

    /**
     * 客户端当前状态
     */
    @Getter
    private volatile WebsocketClientStatus clientStatus = WebsocketClientStatus.NEW;

    /**
     * clientStatus更新的回调
     */
    @Setter
    private Consumer<WebsocketClientStatus> clientStatusChangeHandler = socketCloseStatus -> {
    };


    /**
     * 关闭时的回调列表
     */
    private final List<WSCloseHandler> closeHandlerList = new ArrayList<>();

    @Setter
    @Getter
    private String name;

    private Bootstrap bootstrap;

    private EventLoopGroup eventLoopGroup;

    private URI uri;

    private String host;

    private int port;

    private boolean useSSL;

    private Channel channel;

    public AbstractWebsocketClient(
            String url,
            AbstractWebSocketClientHandler<P, T> handler
    ) {
        this.url = url;
        this.handler = handler;
        this.handler.websocketClient = this;

        this.callbackInvoker = Executors.newVirtualThreadPerTaskExecutor();
    }

    private void init() throws SSLException, URISyntaxException {

        resolveParamFromUrl();

        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, headers, MAX_FRAME_SIZE
        );
        handler.init(handshaker);

        final SslContext sslCtx;
        if (useSSL) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        bootstrap = new Bootstrap();

        eventLoopGroup = new NioEventLoopGroup();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress(host, port)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 设置连接超时为10秒
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (proxy != null) {
                            // 添加 HttpProxyHandler 作为代理
                            p.addLast(new HttpProxyHandler(proxy.getAddress(), proxy.getUsername(), proxy.getPassword()));
                        }

                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), port));
                        }

                        p.addLast("http-chunked", new ChunkedWriteHandler()); // 支持大数据流


                        p.addLast(new HttpClientCodec());
                        p.addLast(new HttpObjectAggregator(81920));
                        p.addLast(new IdleStateHandler(0, 0, allIdleTimeSecond, TimeUnit.SECONDS));
                        p.addLast(new ChunkedWriteHandler());

                        p.addLast(new WebSocketFrameAggregator(MAX_FRAME_SIZE));  // 设置聚合器的最大帧大小


                        p.addLast(handler);
                    }
                });
    }

    /**
     * 链接服务端
     */
    public CompletableFuture<Boolean> connect() {
        return connect(null);
    }

    /**
     * 链接服务端
     */
    public CompletableFuture<Boolean> connect(WSCloseHandler wsCloseHandler) {

        /*
         * 添加关闭的回调
         */
        if (wsCloseHandler != null) {
            synchronized (closeHandlerList) {
                closeHandlerList.add(wsCloseHandler);
            }
        }

        return switch (clientStatus) {
            case NEW, STOP -> reconnect();
            case STARTING -> waitForStarting();
            case RUNNING -> {
                log.warn("WS客户端[{}}正在运行, clientStatus[{}]", url, clientStatus);
                yield CompletableFuture.supplyAsync(() -> true);
            }
            case SHUTDOWN -> throw new RuntimeException("");
        };
    }


    /**
     * 重链接
     *
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Boolean> reconnect() {
        return switch (clientStatus) {
            case NEW, STOP -> doReconnect();
            case STARTING -> waitForStarting();
            case RUNNING -> {
                log.warn("WS客户端[{}}正在启动或运行, 不能reconnect. clientStatus[{}]", url, clientStatus);
                yield CompletableFuture.supplyAsync(() -> true);
            }
            case SHUTDOWN -> CompletableFuture.supplyAsync(() -> {
                log.error("client[{}] already shutdown", name);
                return false;
            });
        };
    }

    /**
     * 执行重连接，带重试逻辑
     *
     * @return CompletableFuture<Void>
     */
    private CompletableFuture<Boolean> doReconnect() {
        updateClientStatus(WebsocketClientStatus.STARTING);

        return CompletableFuture.supplyAsync(() -> {
            //Step 1 重连次数超过限制，关闭
            if (reconnectTimes.get() >= NettyConstants.RECONNECT_LIMIT) {
                log.error("reconnect times out of limit [{}], close websocket client", NettyConstants.RECONNECT_LIMIT);
                close();
                return false;
            }

            AtomicBoolean isSuccess = new AtomicBoolean(false);

            //Step 2 重连逻辑
            //Step 2.1 加锁保证只要一个线程进行重连
            reconnectLock.lock();
            try {

                //Step 2.2 已经再running状态，直接返回true。变为shutdown、stop状态，直接返回false
                if (clientStatus.equals(WebsocketClientStatus.RUNNING)) {
                    log.info("client started by other thread");
                    return true;
                } else if (clientStatus.equals(WebsocketClientStatus.SHUTDOWN) || clientStatus.equals(WebsocketClientStatus.STOP)) {
                    log.error("clint stop/shutdown when client starting");
                    return false;
                }

                //Step 3 初始化
                log.info("开始初始化WS客户端");
                try {
                    init();
                } catch (SSLException | URISyntaxException e) {
                    throw new RuntimeException("初始化WS客户端发生错误", e);
                }
                log.info("初始化WS客户端完成，开始链接服务器 [{}]", url);


                //Step 4 链接服务器
                if (reconnectTimes.incrementAndGet() <= NettyConstants.RECONNECT_LIMIT) {

                    //Step 4.1 每进行重连都会先将次数加1并设置定时任务将重连次数减1
                    eventLoopGroup.schedule(() -> {
                        reconnectTimes.decrementAndGet();
                    }, 180, TimeUnit.SECONDS);

                    log.info("start connect client [{}], url[{}], current times [{}]", name, url, reconnectTimes.get());

                    //Step 4.2 latch用于同步等等链接完成
                    CountDownLatch latch = new CountDownLatch(1);

                    //Step 4.3 延迟再进行连接
                    eventLoopGroup.schedule(() -> {
                        try {
                            channel = bootstrap.connect().sync().channel();

                            handler.handshakeFuture().sync();

                            channel.attr(NettyConstants.CLIENT_NAME).set(name);

                            //Step 4.4 连接成功设置标识
                            isSuccess.set(true);
                        } catch (Exception e) {
                            log.error("connect client [{}], url[{}] error, times [{}]", name, url, reconnectTimes.get(), e);
                            isSuccess.set(false);
                        } finally {
                            latch.countDown();
                        }
                    }, NettyConstants.RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);

                    //Step 4.5 等待链接完成
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        log.error("connect client [{}], url[{}] interrupted, times [{}]", name, url, reconnectTimes.get(), e);
                    }
                }
            } catch (Exception e) {
                //exception 遇到未处理异常，直接关闭
                close();
                throw new RuntimeException(String.format("connect client [%s] appear unknown error", name), e);
            } finally {
                //Step 5 释放等待启动的线程
                startingWaitCondition.signalAll();
                reconnectLock.unlock();
            }

            //Step 6 未成功启动，关闭
            if (!isSuccess.get()) {
                log.info("connect client [{}], url[{}] fail, current times [{}]", name, url, reconnectTimes.get());

                close();
            } else {
                log.info("connect client [{}], url[{}] success, current times [{}]", name, url, reconnectTimes.get());

                updateClientStatus(WebsocketClientStatus.RUNNING);
                reconnectTimes.set(0);
            }

            return isSuccess.get();
        }, callbackInvoker);
    }


    /**
     * 停止WebSocketClient
     */
    public void close() {
        synchronized (closeHandlerList) {
            if (clientStatus.equals(WebsocketClientStatus.STOP)) return;

            updateClientStatus(WebsocketClientStatus.STOP);

            log.info("closing websocket client [{}], [{}]", name, channel.hashCode());
            if (channel != null) {
                channel.close();
            }

            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully();
            }


            //执行关闭回调
            Iterator<WSCloseHandler> iterator = closeHandlerList.iterator();
            while (iterator.hasNext()) {
                WSCloseHandler closeHandler = iterator.next();
                iterator.remove();
                CompletableFuture.runAsync(closeHandler::onClosed, callbackInvoker);
            }
        }

        log.warn("web socket client [{}] closed", name);
    }

    /**
     * 彻底关闭客户端
     */
    public void shutdown() {
        close();
        updateClientStatus(WebsocketClientStatus.SHUTDOWN);
        log.warn("web socket client [{}] already shutdown !", name);
    }

    /**
     * 等待启动完成
     *
     * @return CompletableFuture<Void>
     */
    private CompletableFuture<Boolean> waitForStarting() {
        return CompletableFuture.supplyAsync(() -> {
            log.warn("client [{}] is starting, waiting for complete", name);
            reconnectLock.lock();
            try {
                while (clientStatus.equals(WebsocketClientStatus.RUNNING)) {
                    startingWaitCondition.await();
                }

                if (clientStatus.equals(WebsocketClientStatus.STOP) || clientStatus.equals(WebsocketClientStatus.SHUTDOWN)) {
                    log.error("启动WS客户端[{}]失败, ClientStatus [{}}", name, clientStatus);
                    return false;
                }
                return true;
            } catch (InterruptedException e) {
                log.error("waiting for start client [{}] error", name);
                throw new RuntimeException(e);
            } finally {
                reconnectLock.unlock();
            }
        }, callbackInvoker);
    }


    /**
     * 发送消息，没有回调
     *
     * @param message message
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> sendMessage(P message) {
        return CompletableFuture.runAsync(() -> {
            if (message == null) {
                throw new IllegalArgumentException("message is null");
            }

            try {
                channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(message)));
            } catch (Exception e) {
                throw new RuntimeException("send message [" + message + "] error");
            }
        }, callbackInvoker);
    }

    ;

    /**
     * 发送请求, 注册响应监听
     *
     * @param request 请求体
     */
    public CompletableFuture<T> sendRequest(P request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request == null) {
                log.error("request is null");
                return null;
            }

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<T> jb = new AtomicReference<>(null);

            boolean flag = handler.registryRequest(request, response -> {
                latch.countDown();
                jb.set(response);
            });

            if (flag) {
                log.info("send request [{}]", request);
                channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(request)));
                log.debug("send request [{}] success", request);
            } else {
                log.error("request id registered");
                return null;
            }

            try {
                if (!latch.await(NettyConstants.REQUEST_WAITE_SECONDS, TimeUnit.SECONDS)) return null;

                return jb.get();
            } catch (InterruptedException e) {
                log.error("send request interrupted", e);
                return null;
            }
        }, callbackInvoker);
    }

    /**
     * 发送ping
     */
    public void sendPing() {
        log.debug("client [{}] send ping {}", name, url);
        channel.writeAndFlush(new PingWebSocketFrame());
    }

    /**
     * 发送pong
     */
    public void sendPong() {
        log.debug("client [{}] send pong {}", name, url);
        channel.writeAndFlush(new PongWebSocketFrame());
    }


    /**
     * 更新status
     *
     * @param newStatus newStatus
     */
    public void updateClientStatus(WebsocketClientStatus newStatus) {
        synchronized ("CLIENT_STATUS_LOCK") {
            try {
                if (clientStatusChangeHandler != null) {
                    clientStatusChangeHandler.accept(newStatus);
                }
            } finally {
                clientStatus = newStatus;
            }
        }
    }

    /**
     * 解析参数
     *
     * @throws URISyntaxException url解析错误
     */
    private void resolveParamFromUrl() throws URISyntaxException {
        uri = new URI(url);
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            log.error("Only WS(S) is supported.");
            throw new IllegalArgumentException("url error, Only WS(S) is supported.");
        }

        useSSL = "wss".equalsIgnoreCase(scheme);
    }
}
