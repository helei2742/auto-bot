package cn.com.helei.bot.core.supporter.netty.handler;

import cn.com.helei.bot.core.supporter.netty.constants.NettyConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


@Slf4j
public class RequestResponseHandler<T> {

    private final ConcurrentMap<String, HandlerEntity<T>> requestIdMap = new ConcurrentHashMap<>();


    /**
     * 注册request
     *
     * @param id request的id
     * @return 是否注册成功
     */
    public boolean registryRequest(String id, Consumer<T> callback) {
        AtomicBoolean res = new AtomicBoolean(false);
        requestIdMap.compute(id, (k, v) -> {
            if (v == null) {
                res.set(true);
                long expireTime = System.currentTimeMillis() + NettyConstants.REQUEST_WAITE_SECONDS * 1000;
                v = new HandlerEntity<>(expireTime, callback);
                log.debug("registry request id[{}] success, expire time [{}]", id, expireTime);
            }
            return v;
        });

        return res.get();
    }

    /**
     * 提交resoonse
     *
     * @param id       id
     * @param response response
     */
    public boolean submitResponse(String id, T response) {
        HandlerEntity<T> entity = requestIdMap.get(id);
        if (entity == null) {
            log.warn("request id[{}} didn't exist", id);
            return false;
        } else {
            long currentTimeMillis = System.currentTimeMillis();
            if (entity.expireTime < currentTimeMillis) {
                log.warn("request id[{}] expired, expire time[{}], currentTime[{}] cancel invoke callback",
                        id, entity.expireTime, currentTimeMillis);
                return false;
            } else {
                entity.callback.accept(response);
                log.debug("invoke request id[{}] callback success", id);
                return true;
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    static class HandlerEntity<T> {
        private long expireTime;

        private Consumer<T> callback;
    }
}
