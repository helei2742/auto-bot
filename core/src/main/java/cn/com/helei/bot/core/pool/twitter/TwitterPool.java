package cn.com.helei.bot.core.pool.twitter;

import cn.com.helei.bot.core.pool.AbstractYamlLinePool;

public class TwitterPool extends AbstractYamlLinePool<Twitter> {

    public TwitterPool() {
        super(Twitter.class);
    }

    public static TwitterPool getDefault() {
        return loadYamlPool(
                "bot/twitter.yaml",
                "bot.twitter",
                TwitterPool.class
        );
    }
}
