package cn.com.helei.DepinBot.core.bot.extension;

import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketBotExtension implements BotExtension {

    @Override
    public Type extensionType() {
        return Type.MENU;
    }

    @Override
    public Method targetMethod() {
        return null;
    }


}
