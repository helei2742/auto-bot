package cn.com.helei.bot.core.bot.extension;

import java.lang.reflect.Method;

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
