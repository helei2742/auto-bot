package cn.com.helei.bot.core.bot.job;

import cn.com.helei.bot.core.bot.anno.BotWSMethodConfig;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import lombok.*;
import org.quartz.CronExpression;

import java.lang.reflect.Method;

@Data
@AllArgsConstructor
public class AutoBotJobParam {

    private final AnnoDriveAutoBot<?> bot;

    private final String jobName;

    @Getter
    private final String description;

    private final Method jobMethod;

    @Getter
    private final CronExpression cronExpression;

    @Getter
    private final Integer intervalInSecond;

    private final int concurrentCount;

    private final BotWSMethodConfig botWSMethodConfig;

    private Object target;

    private Object[] extraParams;

    public String getGroup() {
        return this.bot.getBotInfo().getName();
    }

    public Integer getBotId() {
        return bot.getBotInfo().getId();
    }
}
