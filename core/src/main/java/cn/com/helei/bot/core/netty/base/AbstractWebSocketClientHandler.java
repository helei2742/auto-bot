package cn.com.helei.bot.core.netty.base;

import cn.com.helei.bot.core.netty.constants.NettyConstants;
import io.netty.channel.*;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/**
 * WebSocket客户端处理器抽象类
 * 能够处理请求响应类型的消息。
 * 其它类型的消息要通过handleOtherMessage()抽象方法处理
 *
 * @param <Resp>
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractWebSocketClientHandler<Req, Resp> extends BaseWebSocketClientHandler<Req, Resp> {

    /**
     * 存放请求响应的回调
     */
    protected final ConcurrentMap<Object, HandlerEntity<Resp>> requestIdMap = new ConcurrentHashMap<>();


    @Override
    protected void whenReceiveMessage(String text) {
        Resp message = convertMessageToRespType(text);

        Object responseId = getResponseId(message);

        if (responseId != null) {
            //有id，是发送请求的响应
            //提交response
            handleResponseMessage(responseId, message);
        } else {
            //没有id，按其它格式处理
            handleOtherMessage(message);
        }
    }


    /**
     * 注册request
     *
     * @param request request
     * @return 是否注册成功
     */
    public boolean registryRequest(Req request, Consumer<Resp> callback) {
        AtomicBoolean res = new AtomicBoolean(false);
        Object requestId = getRequestId(request);

        if (requestId == null) return false;

        requestIdMap.compute(requestId, (k, v) -> {
            if (v == null) {
                res.set(true);
                long expireTime = System.currentTimeMillis() + NettyConstants.REQUEST_WAITE_SECONDS * 1000;
                v = new HandlerEntity<>(expireTime, callback);
                log.debug("registry request id[{}] success, expire time [{}]", requestId, expireTime);
            }
            return v;
        });

        return res.get();
    }

    /**
     * 处理请求响应的消息
     *
     * @param id       id
     * @param response 响应消息体
     */
    protected void handleResponseMessage(Object id, Resp response) {
        HandlerEntity<Resp> handlerEntity = requestIdMap.get(id);

        if (System.currentTimeMillis() > handlerEntity.getExpireTime()) {
            log.warn("请求[{}]得到响应超时", id);
        } else {
            websocketClient.callbackInvoker.execute(() -> handlerEntity.getCallback().accept(response));
        }
    }

    /**
     * 处理其他类型消息
     *
     * @param message 消息
     */
    protected abstract void handleOtherMessage(Resp message);


    /**
     * 将websocket收到的文本消息转换为响应类型 Resp
     *
     * @param message websocket收到的原始消息
     * @return typedMessage
     */
    public abstract Resp convertMessageToRespType(String message);


    /**
     * 获取请求id
     *
     * @param request request
     * @return id
     */
    public abstract Object getRequestId(Req request);

    /**
     * 获取响应id
     *
     * @param response 响应
     * @return id
     */
    public abstract Object getResponseId(Resp response);
}
