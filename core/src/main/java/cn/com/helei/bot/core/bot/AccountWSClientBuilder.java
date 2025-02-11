package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;

import java.lang.reflect.InvocationTargetException;

public interface AccountWSClientBuilder {

    BaseBotWSClient<?, ?> build(AccountContext accountContext) throws InvocationTargetException, IllegalAccessException;

}
