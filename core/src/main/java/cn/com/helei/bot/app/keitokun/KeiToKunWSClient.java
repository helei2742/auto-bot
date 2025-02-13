package cn.com.helei.bot.app.keitokun;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;
import cn.com.helei.bot.core.supporter.netty.BotJsonWSClient;
import cn.com.helei.bot.core.supporter.netty.constants.WebsocketClientStatus;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static cn.com.helei.bot.app.keitokun.KeiToKunBot.TODAY_KEY;
import static cn.com.helei.bot.app.keitokun.KeiToKunBot.TODAY_REMAINING_TAP_KEY;


@Slf4j
public class KeiToKunWSClient extends BotJsonWSClient {

    private final static Map<BaseBotWSClient<JSONObject, JSONObject>, Integer> clientNoResponsePingCount = new ConcurrentHashMap<>();

    private final static Map<BaseBotWSClient<JSONObject, JSONObject>, Integer> requestIdMap = new ConcurrentHashMap<>();

    private final static int noResponsePingLimit = 10;

    private final Random random = new Random();

    public KeiToKunWSClient(AccountContext accountContext, String connectUrl) {
        super(accountContext, connectUrl);
    }

    @Override
    public void whenAccountClientStatusChange(WebsocketClientStatus clientStatus) {
        AccountContext accountContext = this.getAccountContext();

        String printPrefix = accountContext.getSimpleInfo() + "-" + accountContext.getParam(KeiToKunBot.UID_KEY);

        switch (clientStatus) {
            case RUNNING -> {
                log.info("{} 连接ws服务器[{}]成功", printPrefix, url);

            }
            case STOP -> {
                log.info("{} 连接到ws服务器[{}]失败", printPrefix, url);

                accountContext.getConnectStatusInfo().getRestart().incrementAndGet();
            }
            case SHUTDOWN -> {
                log.info("{} ws连接已断开", printPrefix);
                requestIdMap.remove(this);
            }
        }
    }

    @Override
    public JSONObject getHeartbeatMessage() {
        Integer count = clientNoResponsePingCount.compute(this, (k, v) -> {
            if (v != null && v >= noResponsePingLimit) {
                return null;
            }
            return v == null ? 1 : v + 1;
        });

        if (count == null) {
            log.warn("{} 长时间未收到pong，关闭客户端", this.getAccountContext().getSimpleInfo());
            this.close();
            return null;
        }


        JSONObject frame = new JSONObject();
        frame.put("cmd", 1001);
        frame.put("id", requestIdMap.compute(this, (k, v) -> v == null ? 1 : v + 1));
        frame.put("uid", getAccountContext().getParam("uid"));

        JSONObject data = new JSONObject();
        int randomClickTimes = getRandomClickTimes();

        data.put("amount", randomClickTimes);
        data.put("collectNum", randomClickTimes);
        data.put("timestamp", System.currentTimeMillis());
        frame.put("data", data);

        log.info("[{}] 发送心跳[{}]", getAccountContext().getSimpleInfo(), frame);
        getAccountContext().getConnectStatusInfo().getHeartBeat().getAndIncrement();

        return frame;
    }

    @Override
    public void whenAccountReceiveResponse(
            Object id,
            JSONObject response
    ) {
        Integer cmd = response.getInteger("cmd");
        JSONObject data = response.getJSONObject("data");

        AccountContext accountContext = this.getAccountContext();
        ConnectStatusInfo connectStatusInfo = accountContext.getConnectStatusInfo();

        String prefix = accountContext.getSimpleInfo() + "-" + accountContext.getParam("uid");

        log.info("{} 收到消息 {}", prefix, response);
        if (cmd == 1001) {
            Integer totalNum = data.getInteger("totalNum");
            Integer collectNum = data.getInteger("collectNum");

            accountContext.setParam(TODAY_REMAINING_TAP_KEY,
                    String.valueOf(Math.max(0, totalNum - collectNum)));

            // 今日的领完了, 关闭
            if (totalNum <= collectNum) {
                log.info("{} 今日keitokun点击已完成，断开ws连接", prefix);
                accountContext.setParam(TODAY_KEY, LocalDate.now().toString());
                this.shutdown();
            }

            log.info("{} 收到响应,[{}/{}](已点击/剩余)", prefix,
                    collectNum, totalNum);

            accountContext.getRewordInfo().setTotalPoints(data.getInteger("keitoAmount") * 1.0);
            accountContext.getRewordInfo().setTotalPoints(collectNum * 1.0);

            connectStatusInfo.getHeartBeat().incrementAndGet();
        } else {
            log.warn("{} 收到未知响应[{}]", prefix, response);
            connectStatusInfo.getErrorHeartBeat().incrementAndGet();
        }

        connectStatusInfo.setUpdateDateTime(LocalDateTime.now());
    }

    @Override
    public void whenAccountReceiveMessage(
            JSONObject message
    ) {

    }


    private int getRandomClickTimes() {
        return random.nextInt(5) + 1;
    }
}
