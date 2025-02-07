package cn.com.helei.bot.core.bot.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotApplication {

    /**
     * bot 名字
     *
     * @return name
     */
    String name();

    /**
     * 描述
     *
     * @return 描述
     */
    String describe() default "";

    /**
     * 适用项目的id
     *
     * @return id
     */
    int[] limitProjectIds() default {};
}
