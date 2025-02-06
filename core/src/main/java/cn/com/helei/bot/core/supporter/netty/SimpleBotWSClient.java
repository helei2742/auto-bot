package cn.com.helei.bot.core.supporter.netty;

import cn.com.helei.bot.core.bot.WSTaskAutoBot;
import cn.com.helei.bot.core.config.BaseAutoBotConfig;
import cn.com.helei.bot.core.entity.AccountContext;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;


public class SimpleBotWSClient extends BaseBotWSClient<JSONObject, JSONObject> {

    private final WSTaskAutoBot<?, JSONObject, JSONObject> bot;

    @Setter
    private String idFieldName = "id";

    public SimpleBotWSClient(
            WSTaskAutoBot<? extends BaseAutoBotConfig, JSONObject, JSONObject> bot,
            AccountContext accountContext
    ) {
        super(accountContext, new SimpleBotWSClientHandler());
        this.bot = bot;
        ((SimpleBotWSClientHandler) handler).setWsClient(this);
    }

    @Override
    public JSONObject getHeartbeatMessage(BaseBotWSClient<JSONObject, JSONObject> wsClient) {
        return bot.getHeartbeatMessage(wsClient);
    }

    @Override
    public void whenAccountReceiveResponse(BaseBotWSClient<JSONObject, JSONObject> wsClient, Object id, JSONObject response) {
        bot.whenAccountReceiveResponse(wsClient, id, response);
    }

    @Override
    public void whenAccountReceiveMessage(BaseBotWSClient<JSONObject, JSONObject> wsClient, JSONObject message) {
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
    private static class SimpleBotWSClientHandler extends BaseBotWSClientHandler<JSONObject, JSONObject> {

        private SimpleBotWSClient wsClient;

        @Override
        public JSONObject convertMessageToRespType(String message) {
            return JSONObject.parseObject(message);
        }
    }
}
