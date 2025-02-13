package cn.com.helei.bot.core.supporter.botapi.impl;

import cn.com.helei.bot.core.bot.job.AutoBotJob;
import cn.com.helei.bot.core.bot.job.AutoBotJobParam;
import cn.com.helei.bot.core.dto.BotACJobResult;
import cn.com.helei.bot.core.supporter.botapi.BotJobService;
import org.quartz.*;
        import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cn.com.helei.bot.core.bot.job.AutoBotJob.BOT_JOB_PARAM_Key;

@Component
public class QuartzBotJobService implements BotJobService {

    private static final Logger log = LoggerFactory.getLogger(QuartzBotJobService.class);

    @Autowired
    private Scheduler scheduler;

    @Override
    public List<BotACJobResult> registerJobList(Collection<AutoBotJobParam> autoBotJobParams) {

        List<BotACJobResult> resultList = new ArrayList<>(autoBotJobParams.size());

        for (AutoBotJobParam autoBotJob : autoBotJobParams) {
            resultList.add(registerJob(autoBotJob));
        }

        return resultList;
    }


    @Override
    public BotACJobResult registerJob(AutoBotJobParam jobParam) {
        BotACJobResult result = BotACJobResult.builder()
                .botId(jobParam.getBotId())
                .group(jobParam.getGroup())
                .jobName(jobParam.getJobName())
                .success(true)
                .build();

        JobKey jobKey = new JobKey(jobParam.getJobName(), jobParam.getGroup());

        try {
            // 存在这个job
            if (scheduler.checkExists(jobKey)) {
                result.setSuccess(false);
                result.setErrorMsg("job exist");
            } else {
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put(BOT_JOB_PARAM_Key, jobParam);

                // 不存在，创建并运行
                JobDetail jobDetail = JobBuilder.newJob(AutoBotJob.class)
                        .withIdentity(jobKey)
                        .withDescription(jobParam.getDescription())
                        .setJobData(jobDataMap)
                        .storeDurably()
                        .build();

                TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                        .withIdentity(jobParam.getJobName(), jobParam.getGroup())
                        .startNow();

                if (jobParam.getIntervalInSecond() != null) {
                    triggerBuilder
                            .withSchedule(SimpleScheduleBuilder
                                    .simpleSchedule()
                                    .withIntervalInSeconds(jobParam.getIntervalInSecond())
                                    .repeatForever()
                            );
                } else if (jobParam.getCronExpression() != null) {
                    triggerBuilder
                            .withSchedule(CronScheduleBuilder.cronSchedule(jobParam.getCronExpression()));
                }

                scheduler.scheduleJob(jobDetail, triggerBuilder.build());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());

            log.error("注册[{}]job发生异常", jobKey, e);
        }

        return result;
    }

}
