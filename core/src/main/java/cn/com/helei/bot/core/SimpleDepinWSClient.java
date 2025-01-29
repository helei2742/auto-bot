package cn.com.helei.bot.core;

import cn.com.helei.bot.core.bot.WSMenuCMDLineDepinBot;
import cn.com.helei.bot.core.config.BaseDepinBotConfig;
import cn.com.helei.bot.core.dto.account.AccountContext;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;


public class SimpleDepinWSClient extends BaseDepinWSClient<JSONObject, JSONObject> {

    private final WSMenuCMDLineDepinBot<?, JSONObject, JSONObject> bot;

    @Setter
    private String idFieldName = "id";

    public SimpleDepinWSClient(
            WSMenuCMDLineDepinBot<? extends BaseDepinBotConfig, JSONObject, JSONObject> bot,
            AccountContext accountContext
    ) {
        super(accountContext, new SimpleDepinWSClientHandler());
        this.bot = bot;
        ((SimpleDepinWSClient.SimpleDepinWSClientHandler) handler).setWsClient(this);
    }

    @Override
    public JSONObject getHeartbeatMessage(BaseDepinWSClient<JSONObject, JSONObject> wsClient) {
        return bot.getHeartbeatMessage(wsClient);
    }

    @Override
    public void whenAccountReceiveResponse(BaseDepinWSClient<JSONObject, JSONObject> wsClient, Object id, JSONObject response) {
        bot.whenAccountReceiveResponse(wsClient, id, response);
    }

    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<JSONObject, JSONObject> wsClient, JSONObject message) {
        bot.whenAccountReceiveMessage(wsClient, message);
    }

    @Override
    public Object getRequestId(JSONObject request) {
        return request.get(idFieldName);
    }

    @Override
    public Object getResponseId(JSONObject response) {
        return response.get(idFieldName);
    }


    @Setter
    @Getter
    private static class SimpleDepinWSClientHandler extends BaseDepinWSClientHandler<JSONObject, JSONObject> {

        private SimpleDepinWSClient wsClient;

        @Override
        public JSONObject convertMessageToRespType(String message) {
            return JSONObject.parseObject(message);
        }
    }
}
