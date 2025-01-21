package cn.com.helei.DepinBot.openLedger;


import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.dto.account.ConnectStatusInfo;
import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.BaseDepinWSClientHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * OpenLedgerDepinClient
 */
@Slf4j
public class OpenLedgerDepinWSClient extends BaseDepinWSClient<String, String> {

    public OpenLedgerDepinWSClient(AccountContext accountContext) {
        super(accountContext, new OpenLedgerDepinClientHandler(accountContext));
    }

    @Override
    public String getHeartbeatMessage(BaseDepinWSClient<String, String> wsClient) {
        return "";
    }

    @Override
    public void whenAccountReceiveResponse(BaseDepinWSClient<String, String> wsClient, String id, String response) {

    }

    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<String, String> wsClient, String message) {

    }


    /**
     * OpenLedgerDepinClient 的 Netty Handler
     */
    public static class OpenLedgerDepinClientHandler extends BaseDepinWSClientHandler<String, String> {

        private final Random random = new Random();

        private final AccountContext accountContext;

        public OpenLedgerDepinClientHandler(AccountContext accountContext) {
            this.accountContext = accountContext;
        }

        @Override
        protected void handleOtherMessage(String message) {

        }

        @Override
        public String convertMessageToRespType(String message) {
            return message;
        }


        protected String heartBeatMessage() {
            //发送心跳时更新状态
            ConnectStatusInfo statusInfo = accountContext.getConnectStatusInfo();

            statusInfo.setUpdateDateTime(LocalDateTime.now());
            statusInfo.getHeartBeatCount().incrementAndGet();

            JSONObject heartBeatMessage = buildHeartBeatMessageContext();

            return heartBeatMessage.toJSONString();
        }


        /**
         * 构建心跳消息体
         *
         * @return JSONObject
         */
        private @NotNull JSONObject buildHeartBeatMessageContext() {
            OpenLedgerConfig.OpenLedgerAccount openLedgerAccount = (OpenLedgerConfig.OpenLedgerAccount)
                    accountContext.getClientAccount();


            JSONObject heartBeatMessage = new JSONObject();
            heartBeatMessage.put("msgType", "HEARTBEAT");
            heartBeatMessage.put("workerType", "LWEXT");
            heartBeatMessage.put("workerID", openLedgerAccount.getIdentity());

            JSONObject message = new JSONObject();
            JSONObject worker = new JSONObject();
            worker.put("Identity", openLedgerAccount.getIdentity());
            worker.put("ownerAddress", openLedgerAccount.getOwnerAddress());
            worker.put("type", "LWEXT");
            worker.put("Host", openLedgerAccount.getOpenLedgerConfig().getOrigin());
            message.put("Worker", worker);

            JSONObject capacity = new JSONObject();
            capacity.put("AvailableMemory", randomAM(0.5, 0.99));
            capacity.put("AvailableStorage", "99.99");
            capacity.put("AvailableGPU", "");
            capacity.put("AvailableModels", new JSONArray());

            message.put("Capacity", capacity);
            heartBeatMessage.put("message", message);
            return heartBeatMessage;
        }


        /**
         * 获取随机AvailableMemory
         */
        public double randomAM(double min, double max) {
            // 生成范围内的两位小数
            double randomValue = min + random.nextDouble((max - min) * 100); // 乘以 100 是为了得到两位小数
            randomValue = Math.round(randomValue * 100.0) / 100.0; // 四舍五入并保留两位小数
            return randomValue;
        }
    }

}
