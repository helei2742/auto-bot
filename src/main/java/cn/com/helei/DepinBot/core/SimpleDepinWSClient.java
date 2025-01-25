package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.bot.WSMenuCMDLineDepinBot;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;


public class SimpleDepinWSClient extends BaseDepinWSClient<JSONObject, JSONObject> {

    private final WSMenuCMDLineDepinBot<?, JSONObject, JSONObject> bot;

    public SimpleDepinWSClient(
            WSMenuCMDLineDepinBot<?, JSONObject, JSONObject> bot,
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
    public void whenAccountReceiveResponse(BaseDepinWSClient<JSONObject, JSONObject> wsClient, String id, JSONObject response) {
        bot.whenAccountReceiveResponse(wsClient, id, response);
    }

    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<JSONObject, JSONObject> wsClient, JSONObject message) {
        bot.whenAccountReceiveMessage(wsClient, message);
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
