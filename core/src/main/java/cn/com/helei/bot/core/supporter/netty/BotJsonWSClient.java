package cn.com.helei.bot.core.supporter.netty;

import cn.com.helei.bot.core.entity.AccountContext;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

@Setter
public abstract class BotJsonWSClient extends BaseBotWSClient<JSONObject, JSONObject> {

    private String idFieldName = "id";

    public BotJsonWSClient(
            AccountContext accountContext,
            String connectUrl
    ) {
        super(accountContext, connectUrl, new SimpleBotWSClientHandler());
        ((SimpleBotWSClientHandler) handler).setWsClient(this);
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

        private BotJsonWSClient wsClient;

        @Override
        public JSONObject convertMessageToRespType(String message) {
            return JSONObject.parseObject(message);
        }
    }
}
