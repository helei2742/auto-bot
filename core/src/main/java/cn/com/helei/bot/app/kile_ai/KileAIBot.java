package cn.com.helei.bot.app.kile_ai;

import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.entity.ProxyInfo;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.util.FileUtil;
import cn.com.helei.bot.core.util.pool.IdMarkPool;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@BotApplication(name = "Kile_AI_BOT")
public class KileAIBot extends AnnoDriveAutoBot<KileAIBot> {


    private static final List<String> AGENT_LIST = List.of(
            "deployment-uu9y1z4z85rapgwkss1muuiz",
            "deployment-ecz5o55dh0dbqagkut47kzyc",
            "deployment-sofftlsf9z4fya3qchykaanq"
    );

    private static final String QUESTION_CONFIRM_URL = "https://quests-usage-dev.prod.zettablock.com/api/report_usage";

    private static final String WALLET_KEY = "eth_address";

    private static final String TODAY_KEY = "today";

    private static final String TODAY_TOTAL_KEY = "today_total";

    private static final int QUESTION_CONFIRM_LIMIT = 3;

    private final Random random = new Random();

    private final IdMarkPool<String> questionPool;


    public KileAIBot(AutoBotConfig autoBotConfig, BotApi botApi) {
        super(autoBotConfig, botApi);

        String botAppConfigPath = FileUtil.getBotAppConfigPath();

        try {
            List<String> questions = Files.readAllLines(Path.of(botAppConfigPath + File.separator + "kile_ai" + File.separator + "question.txt"));
            this.questionPool = IdMarkPool.create(questions, String.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected KileAIBot getInstance() {
        return this;
    }

    /**
     * 每日询问的定时任务
     *
     * @param accountContext accountContext
     * @return String
     */
    @BotMethod(
            jobType = BotJobType.TIMED_TASK,
            jobName = "每日询问 AI",
            intervalInSecond = 60 * 60 * 12
    )
    public String queryAgentClaim(AccountContext accountContext) {
        if (accountContext.getAccountBaseInfo().getId() != 1) return "";

        // Step 1 过滤完成的，或不能用的
        if (filterAccountContext(accountContext)) {
            return "account-%s claim error, account claimed or unusable".formatted(accountContext.getSimpleInfo());
        }

        // Step 2 获取需要查询的数量
        int queryCount = Integer.parseInt(accountContext.getParam(TODAY_TOTAL_KEY));

        int errorCount = 0;

        // Step 3 开始逐个查询
        for (int i = 0; i < queryCount; i++) {
            String agent = getRandomAgent();
            String question = questionPool.getLessUsedItem(1).getFirst();

            log.info("{} 开始询问Agent[{}], [{}/{}]", accountContext.getSimpleInfo(), agent, i + 1, queryCount);

            // Step 3.1 询问问题
            CompletableFuture<Boolean> future = askQuestion(accountContext, agent, question)
                    // Step 3.2 询问完成后，提交问题和答案
                    .thenApplyAsync(resultStr -> {
                        if (StrUtil.isNotBlank(resultStr)) {
                            log.info("{} 询问Agent成功 [{}] -> [{}]", accountContext.getSimpleInfo(), question, resultStr);
                            try {
                                return confirmQuestion(accountContext, agent, question, resultStr).get();
                            } catch (InterruptedException | ExecutionException e) {
                                log.error("{} 上报问答发生异常, {}", accountContext, e.getMessage());
                                return false;
                            }
                        } else {
                            log.error("{} 询问Agent失败, question.dat [{}]", accountContext, question);
                            return false;
                        }
                    });

            try {
                if (future.get()) {
                    log.info("{} - 问题提交成功, [{}/{}]", accountContext.getSimpleInfo(), i + 1, queryCount);
                }
            } catch (InterruptedException | ExecutionException e) {
                errorCount++;
                log.error("{} - 问题提交失败, [{}/{}]", accountContext.getSimpleInfo(), i + 1, queryCount, e);
            }
        }

        return "account-%s-claim-complete, [%d/%d]".formatted(accountContext.getSimpleInfo(), queryCount - errorCount, queryCount);
    }


    /**
     * 过滤不可用的账户
     *
     * @param accountContext accountContext
     * @return boolean 是否过滤
     */
    private boolean filterAccountContext(AccountContext accountContext) {
        if (StrUtil.isBlank(accountContext.getParam(WALLET_KEY))) {
            log.warn("{} 没有钱包参数", accountContext.getSimpleInfo());
            return true;
        }

        String today = LocalDate.now().toString();
        String acDay = accountContext.getParam(TODAY_KEY);

        // 今天没做过
        if (!today.equals(acDay)) {
            accountContext.setParam(TODAY_KEY, today);
            int dailyQueryCount = getDailyQueryCount();
            accountContext.setParam(TODAY_TOTAL_KEY, dailyQueryCount);

            log.info("{} 今日还未执行, 问题个数[{}]", accountContext.getSimpleInfo(), dailyQueryCount);
        } else {
            String todayTotalStr = accountContext.getParam(TODAY_TOTAL_KEY);
            Integer todayTotal = null;
            if (todayTotalStr == null) {
                accountContext.setParam(TODAY_TOTAL_KEY, getDailyQueryCount());
            } else if ((todayTotal = Integer.valueOf(todayTotalStr)) == 0) {
                log.warn("{} 今日已完成", accountContext.getSimpleInfo());
                return true;
            } else {
                log.info("{} 今日剩余[{}], 继续执行", accountContext.getSimpleInfo(), todayTotal);
            }
        }

        return false;
    }


    /**
     * 询问Agent
     *
     * @param accountContext accountContext
     * @param agent          agent
     * @param question       question
     * @return CompletableFuture<String>
     */
    private CompletableFuture<String> askQuestion(AccountContext accountContext, String agent, String question) {
        JSONObject body = new JSONObject();
        body.put("message", question);
        body.put("stream", true);

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("Origin", "https://agents.testnet.gokite.ai");
        headers.put("Referer", "https://agents.testnet.gokite.ai/");
        headers.put("connection", "keep-alive");
        headers.put("Accept", "text/event-stream");

        String url = "https://%s.stag-vxzy.zettablock.com/main".formatted(agent);

        headers.put("Host", url.replace("/main", "")
                .replace("https://", ""));

        return syncStreamRequest(
                accountContext.getProxy(),
                url,
                "post",
                headers,
                null,
                body,
                () -> accountContext.getSimpleInfo() + " 询问Agent - " + question
        ).thenApplyAsync(responseList->{

            log.warn(responseList.toString());
            return "";
        });
    }


    /**
     * 确认问答，，只有确认了才有分
     *
     * @param accountContext accountContext
     * @param agent          agent
     * @param question       question.dat
     * @param answer         answer
     * @return 是否确认成功
     */
    private CompletableFuture<Boolean> confirmQuestion(
            AccountContext accountContext,
            String agent,
            String question,
            String answer
    ) {
        ProxyInfo proxy = accountContext.getProxy();

        String wallet = accountContext.getParam(WALLET_KEY);

        JSONObject body = new JSONObject();
        body.put("wallet_address", wallet);
        body.put("agent_id", agent);
        body.put("request_text", question);
        body.put("response_text", answer);
        body.put("request_metadata", new JSONObject());

        return CompletableFuture.supplyAsync(() -> {
            Exception lastException = null;

            for (int i = 0; i < QUESTION_CONFIRM_LIMIT; i++) {
                try {
                    String resultStr = syncRequest(
                            proxy,
                            QUESTION_CONFIRM_URL,
                            "post",
                            accountContext.getBrowserEnv().getHeaders(),
                            null,
                            body,
                            () -> accountContext.getSimpleInfo() + " 上报问答 - " + question
                    ).get();

                    log.info("{} 上报问答成功, {}", accountContext.getSimpleInfo(), resultStr);
                    return true;
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("{} 上报问答发生异常, [{}/{}]", accountContext.getSimpleInfo(), i + 1, QUESTION_CONFIRM_LIMIT);
                    lastException = e;
                }
            }
            throw new RuntimeException("上报问答超过次数限制，" + QUESTION_CONFIRM_LIMIT, lastException);
        });
    }

    /**
     * 获取每日运行多少次
     *
     * @return 数量
     */
    private int getDailyQueryCount() {
        return 20 + random.nextInt(3);
    }

    /**
     * 随机选取agent
     *
     * @return String
     */
    private String getRandomAgent() {
        return AGENT_LIST.get(random.nextInt(AGENT_LIST.size()));
    }
}
