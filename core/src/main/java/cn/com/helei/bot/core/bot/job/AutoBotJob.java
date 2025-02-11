package cn.com.helei.bot.core.bot.job;

import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class AutoBotJob extends QuartzJobBean {

    public static final String BOT_JOB_PARAM_Key = "bot_job_param";

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        AutoBotJobParam autoBotJobParam = (AutoBotJobParam) context.getJobDetail().getJobDataMap().get(BOT_JOB_PARAM_Key);

        String jobName = autoBotJobParam.getJobName();
        AnnoDriveAutoBot<?> bot = autoBotJobParam.getBot();
        Method jobMethod = autoBotJobParam.getJobMethod();

        Object target = autoBotJobParam.getTarget() == null ? bot : autoBotJobParam.getTarget();
        Object[] extraParams = autoBotJobParam.getExtraParams();

        log.info("开始执行[{}]定时任务", jobName);

        bot.asyncForACList(
                accountContext -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Object[] params;
                        if (extraParams == null) {
                            params = new Object[]{accountContext};
                        } else {
                            params = new Object[1 + extraParams.length];
                            params[0] = accountContext;
                            System.arraycopy(extraParams, 0, params, 1, extraParams.length);
                        }

                        jobMethod.setAccessible(true);
                        Object invoke = jobMethod.invoke(target, params);
                        return Result.ok(invoke);
                    } catch (Exception e) {
                        log.info("执行定时任务发生异常", e);
                        return Result.fail("执行定时任务发生异常" + e.getMessage());
                    }
                }, bot.getExecutorService()),
                (accountContext, result) -> result,
                jobName
        ).thenAcceptAsync(acListOptResult -> {
            if (!acListOptResult.getSuccess()) {
                log.info("botId[{}]-botName[{}]-jobName[{}] 定时任务执行失败, {}",
                        acListOptResult.getBotId(), acListOptResult.getBotName(), acListOptResult.getJobName(), acListOptResult.getErrorMsg()
                );
            } else {
                log.info("botId[{}]-botName[{}]-jobName[{}] 定时任务执行成功, {}/{}",
                        acListOptResult.getBotId(), acListOptResult.getBotName(),
                        acListOptResult.getJobName(), acListOptResult.getSuccessCount(), acListOptResult.getResults().size()
                );
            }
        });

        log.info("[{}]定时任务执行完毕", jobName);
    }
}
