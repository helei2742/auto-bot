package cn.com.helei.DepinBot.core.bot.extension;

import java.lang.reflect.Method;

public interface BotExtension {

    Type extensionType();

    Method targetMethod();

    enum Type {
        MENU,
    }
}
