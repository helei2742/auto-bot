package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.dto.AccountContext;
import com.alibaba.fastjson.JSONObject;

public class SimpleDepinWSClient extends AbstractDepinWSClient<JSONObject, JSONObject> {

    public SimpleDepinWSClient(AccountContext accountContext) {
        super(accountContext, new SimpleDepinWSClientHandler());
    }




    public static class SimpleDepinWSClientHandler extends AbstractDepinWSClientHandler<JSONObject, JSONObject> {

        @Override
        protected JSONObject heartBeatMessage() {
            return null;
        }

        @Override
        protected void handleOtherMessage(JSONObject message) {

        }

        @Override
        public JSONObject convertMessageToRespType(String message) {
            return null;
        }
    }
}
