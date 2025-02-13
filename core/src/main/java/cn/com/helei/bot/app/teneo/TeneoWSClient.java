package cn.com.helei.bot.app.teneo;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.netty.BotJsonWSClient;
import cn.com.helei.bot.core.supporter.netty.constants.WebsocketClientStatus;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TeneoWSClient extends BotJsonWSClient {

    public TeneoWSClient(AccountContext accountContext, String connectUrl) {
        super(accountContext, connectUrl);
    }

    @Override
    public void whenAccountClientStatusChange(WebsocketClientStatus newClientStatus) {

        final AccountContext accountContext = this.getAccountContext();

        switch (newClientStatus) {
            case STARTING -> {
                log.info("账户[{}]-proxy[{}] 开始到ws服务器[{}]",
                        accountContext.getName(), accountContext.getProxy().getAddressStr(), url);
            }
            case RUNNING -> {
                log.info("账户[{}]-proxy[{}]已连接到ws服务器",
                        accountContext.getName(), accountContext.getProxy().getAddressStr());
            }
            case STOP -> {
                log.warn("账户[{}]-proxy[{}]已断开连接",
                        accountContext.getName(), accountContext.getProxy().getAddressStr());

                accountContext.getConnectStatusInfo().getRestart().incrementAndGet();
            }
            case SHUTDOWN ->{
                log.warn("账户[{}]-proxy[{}] 工作已停止",
                        accountContext.getName(), accountContext.getProxy().getAddressStr());
            }
        }
    }

    @Override
    public JSONObject getHeartbeatMessage() {
        ConnectStatusInfo connectStatusInfo = getAccountContext().getConnectStatusInfo();
        connectStatusInfo.getHeartBeat().incrementAndGet();

        // 错误心跳数提前加上
        connectStatusInfo.getErrorHeartBeat().incrementAndGet();

        JSONObject ping = new JSONObject();
        ping.put("type", "ping");
        return ping;
    }

    @Override
    public void whenAccountReceiveResponse(Object id, JSONObject response) {

    }

    @Override
    public void whenAccountReceiveMessage(JSONObject message) {
        String type = message.getString("message");
        AccountContext accountContext = getAccountContext();

        if ("Connected successfully".equals(type)) {
            Double pointsToday = message.getDouble("pointsToday");
            Double pointsTotal = message.getDouble("pointsTotal");

            accountContext.getRewordInfo().setDailyPoints(pointsToday);
            accountContext.getRewordInfo().setTotalPoints(pointsTotal);

            log.info("账户[{}]-proxy[{}] 连接成功. 今日积分: {}, 总积分: {}",
                    accountContext.getName(), accountContext.getProxy().getAddressStr(),
                    pointsToday, pointsTotal);

        } else if ("Pulse from server".equals(type)) {
            Double pointsToday = message.getDouble("pointsToday");
            Double pointsTotal = message.getDouble("pointsTotal");

            accountContext.getRewordInfo().setDailyPoints(pointsToday);
            accountContext.getRewordInfo().setTotalPoints(pointsTotal);

            int heartbeatToday = message.getInteger("heartbeats");

            //减去提前加上的错误心跳数
            accountContext.getConnectStatusInfo().getErrorHeartBeat().decrementAndGet();
            log.debug("账户[{}]-proxy[{}] 心跳发送成功. 今日心跳: {}",
                    accountContext.getName(), accountContext.getProxy().getAddressStr(),
                    heartbeatToday);
        }
    }
}
