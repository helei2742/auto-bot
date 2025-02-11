package cn.com.helei.bot.core.supporter.botapi;

import cn.com.helei.bot.core.bot.job.AutoBotJob;
import cn.com.helei.bot.core.bot.job.AutoBotJobParam;
import cn.com.helei.bot.core.dto.BotACJobResult;

import java.util.Collection;
import java.util.List;

public interface BotJobService {

    /**
     * 批量注册job
     *
     * @param autoBotJobParams autoBotJobParams
     * @return Result
     */
    List<BotACJobResult> registerJobList(Collection<AutoBotJobParam> autoBotJobParams);

    /**
     * 注册job，开始定时执行
     *
     * @param autoBotJobParam autoBotJobParam
     * @return BotACJobResult
     */
    BotACJobResult registerJob(AutoBotJobParam autoBotJobParam);

}
