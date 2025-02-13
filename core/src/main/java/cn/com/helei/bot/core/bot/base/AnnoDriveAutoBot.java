package cn.com.helei.bot.core.bot.base;

import cn.com.helei.bot.core.bot.AccountWSClientBuilder;
import cn.com.helei.bot.core.bot.WebSocketClientLauncher;
import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.bot.job.AutoBotJobParam;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.constants.MapConfigKey;
import cn.com.helei.bot.core.dto.ACListOptResult;
import cn.com.helei.bot.core.dto.BotACJobResult;
import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.entity.BotInfo;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;
import cn.com.helei.bot.core.util.exception.BotMethodFormatException;
import cn.com.helei.bot.core.util.exception.BotMethodInvokeException;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.quartz.CronExpression;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static cn.com.helei.bot.core.constants.MapConfigKey.INVITE_CODE_KEY;

@Slf4j
public abstract class AnnoDriveAutoBot<T> extends AccountManageAutoBot {

    /**
     * jobName -> jobParam
     */
    private final Map<String, AutoBotJobParam> autoBotJobMap;

    /**
     * ws client 启动器
     */
    private final WebSocketClientLauncher webSocketClientLauncher;

    /**
     * job并发控制信号量， jobName -> semaphore
     */
    private final ConcurrentMap<String, Semaphore> jobCCSemaphoreMap;

    /**
     * 注册方法
     */
    @Getter
    private Method registerMethod;

    /**
     * 登录方法
     */
    @Getter
    private Method loginMethod;

    /**
     * 奖励更新方法
     */
    @Getter
    private Method updateRewordMethod;


    public AnnoDriveAutoBot(
            AutoBotConfig autoBotConfig,
            BotApi botApi
    ) {
        super(autoBotConfig, botApi);

        this.webSocketClientLauncher = new WebSocketClientLauncher(this);
        this.jobCCSemaphoreMap = new ConcurrentHashMap<>();

        this.autoBotJobMap = resolveBotMethodAnno();
    }

    /**
     * 构建bot info， 会解析注解查询db，给上层父类调用
     *
     * @return BotInfo
     */
    @Override
    protected BotInfo buildBotInfo() {
        return resolveAnnoBotInfo(getBotApi());
    }

    /**
     * 注册type账号
     *
     * @return String
     */
    @Override
    public CompletableFuture<ACListOptResult> registerAccount() {
        if (registerMethod == null) {
            return CompletableFuture.completedFuture(ACListOptResult.fail(
                    getBotInfo().getId(),
                    getBotInfo().getName(),
                    BotJobType.REGISTER.name(),
                    "未找到注册方法"
            ));
        }

        return asyncForACList(
                accountContext -> {
                    if (BooleanUtil.isTrue(accountContext.isSignUp())) {
                        // 账户注册过，
                        String errorMsg = String.format("[%s]账户[%s]-email[%s]注册过", accountContext.getId(), accountContext.getName(),
                                accountContext.getAccountBaseInfo().getEmail());

                        log.warn(errorMsg);

                        return CompletableFuture.completedFuture(Result.fail(errorMsg));
                    } else if (registerMethod != null) {
                        // 调用注册方法注册
                        return invokeBotMethod(registerMethod, accountContext, getAutoBotConfig().getConfig(INVITE_CODE_KEY));
                    } else {
                        return CompletableFuture.completedFuture(Result.fail("未知错误"));
                    }
                },
                (accountContext, result) -> {
                    // 登录成功
                    if (BooleanUtil.isTrue(result.getSuccess())) {
                        //注册成功
                        AccountContext.signUpSuccess(accountContext);
                    }
                    return result;
                },
                BotJobType.REGISTER.name()
        );
    }


    /**
     * 获取账号的token
     *
     * @return String
     */
    @Override
    public CompletableFuture<ACListOptResult> loginAndTakeTokenAccount() {
        if (registerMethod == null) {
            return CompletableFuture.completedFuture(ACListOptResult.fail(
                    getBotInfo().getId(),
                    getBotInfo().getName(),
                    BotJobType.LOGIN.name(),
                    "未找到登录方法"
            ));
        }

        return asyncForACList(
                accountContext -> invokeBotMethod(loginMethod, accountContext),
                (accountContext, result) -> {
                    // 登录成功
                    if (BooleanUtil.isTrue(result.getSuccess())) {
                        String token = result.getData() == null ? null : (String) result.getData();

                        // token不为空，设置到accountContext里
                        if (StrUtil.isNotBlank(token)) {
                            accountContext.setParam(MapConfigKey.TOKEN_KEY, token);
                        } else {
                            log.debug("账号[{}]-[{}]token为空", accountContext.getId(), accountContext.getName());
                        }
                    }
                    return result;
                },
                BotJobType.LOGIN.name()
        );
    }


    @Override
    public CompletableFuture<ACListOptResult> updateAccountRewordInfo() {
        if (registerMethod == null) {
            return CompletableFuture.completedFuture(ACListOptResult.fail(
                    getBotInfo().getId(),
                    getBotInfo().getName(),
                    BotJobType.QUERY_REWARD.name(),
                    "未找到奖励查询方法"
            ));
        }

        return asyncForACList(
                accountContext -> invokeBotMethod(updateRewordMethod, accountContext),
                (accountContext, result) -> result,
                BotJobType.QUERY_REWARD.name()
        );
    }

    @Override
    public Set<String> getBotJobNameList() {
        return autoBotJobMap.keySet();
    }

    @Override
    public BotACJobResult startBotJob(String jobName) {
        return getBotApi().getBotJobService().registerJob(autoBotJobMap.get(jobName));
    }

    protected abstract T getInstance();

    /**
     * 异步遍历账户
     *
     * @param buildResultFuture buildResultFuture   具体执行的方法
     * @param resultHandler     resultHandler   处理结果的方法
     * @return CompletableFuture<ACListOptResult>
     */
    public CompletableFuture<ACListOptResult> asyncForACList(
            Function<AccountContext, CompletableFuture<Result>> buildResultFuture,
            BiFunction<AccountContext, BotACJobResult, BotACJobResult> resultHandler,
            String jobName
    ) {
        List<AccountContext> accountContexts = getAccountContexts();

        // Step 1 遍历账户，获取执行结果
        List<CompletableFuture<BotACJobResult>> futures = accountContexts.stream()
                .map(accountContext -> {
                    try {
                        // 获取信号量
                        getCcSemaphore(jobName).acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    BotACJobResult botACJobResult;
                    CompletableFuture<Result> future;

                    try {
                        botACJobResult = new BotACJobResult(
                                getBotInfo().getId(),
                                getBotInfo().getName(),
                                jobName,
                                accountContext.getId()
                        );

                        future = buildResultFuture.apply(accountContext);

                    } catch (Exception e) {
                        getCcSemaphore(jobName).release();
                        throw new RuntimeException(e);
                    }

                    return future.thenApplyAsync(botACJobResult::setResult, getExecutorService())
                            .whenComplete((result, throwable) -> {
                                // 释放信号量
                                getCcSemaphore(jobName).release();
                            });
                }).toList();

        // Step 2 等待执行完成，转换执行结果
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(unused -> {
                    List<BotACJobResult> results = new ArrayList<>();

                    int success = 0;
                    for (int i = 0; i < futures.size(); i++) {
                        CompletableFuture<BotACJobResult> future = futures.get(i);
                        AccountContext accountContext = accountContexts.get(i);

                        try {
                            BotACJobResult result = future.get();
                            BotACJobResult botACJobResult = resultHandler.apply(accountContext, result);

                            if (BooleanUtil.isTrue(botACJobResult.getSuccess())) success++;

                            results.add(botACJobResult);
                        } catch (InterruptedException | ExecutionException e) {
                            String errorMsg = String.format("[%s] %s 获取异步结果发生错误",
                                    accountContext.getId(), accountContext.getSimpleInfo());

                            log.error(errorMsg, e);

                            results.add(
                                    new BotACJobResult(
                                            getBotInfo().getId(),
                                            getBotInfo().getName(),
                                            jobName,
                                            accountContext.getId(),
                                            false,
                                            errorMsg,
                                            null
                                    )
                            );
                        }
                    }

                    return ACListOptResult.builder()
                            .botId(getBotInfo().getId())
                            .botName(getBotInfo().getName())
                            .jobName(jobName)
                            .successCount(success)
                            .success(true)
                            .results(results)
                            .build();
                });
    }

    /**
     * 解析注解，添加botInfo
     */
    private BotInfo resolveAnnoBotInfo(BotApi botApi) {
        BotApplication annotation = this.getClass().getAnnotation(BotApplication.class);

        if (annotation != null) {
            String botName = annotation.name();
            if (StrUtil.isBlank(botName)) throw new IllegalArgumentException("bot name 不能为空");

            BotInfo dbBotInfo = botApi.getBotInfoService().query().eq("name", botName).one();

            // 查询bot是否存在，不存在则创建
            if (dbBotInfo == null) {
                log.warn("不存在[{}]bot info, 自动创建...", botName);

                BotInfo botInfo = new BotInfo();
                botInfo.setDescribe(annotation.describe());
                botInfo.setLimitProjectIds(Arrays.toString(annotation.limitProjectIds()));
                botInfo.setName(botName);
                if (botApi.getBotInfoService().save(botInfo)) {
                    log.info("自动创建[{}]bot info成功", botName);
                    return botInfo;
                } else {
                    throw new RuntimeException("保存bot[" + botName + "]信息失败");
                }
            } else {
                return dbBotInfo;
            }
        } else {
            throw new IllegalArgumentException("bot 应该带有 @BotApplication注解");
        }
    }


    /**
     * 解析注解中的方法
     *
     * @return List<AutoBotJob>
     */
    private Map<String, AutoBotJobParam> resolveBotMethodAnno() {
        Map<String, AutoBotJobParam> jobMap = new HashMap<>();

        // Step 1 遍历方法
        for (Method method : getClass().getDeclaredMethods()) {
            method.setAccessible(true);

            // Step 2 找到方法中带有BotMethod注解的
            if (method.isAnnotationPresent(BotMethod.class)) {
                BotMethod botJobMethod = method.getAnnotation(BotMethod.class);

                // Step 3 根据BotMethod注解 的jobType，方法分类
                switch (botJobMethod.jobType()) {
                    case REGISTER -> registerMethodHandler(method);
                    case LOGIN -> loginMethodHandler(method);
                    case QUERY_REWARD -> queryRewardMethodHandler(method, botJobMethod, jobMap);
                    case TIMED_TASK -> timedTaskMethodHandler(method, botJobMethod, jobMap);
                    case WEB_SOCKET_CONNECT -> webSocketConnectMethodHandler(method, botJobMethod, jobMap);
                }
            }
        }

        return jobMap;
    }

    /**
     * 注册方法处理器
     *
     * @param method method
     */
    private void registerMethodHandler(Method method) {
        if (method.getReturnType() == Result.class
                && method.getParameterCount() == 2
                && method.getParameters()[0].getType() == AccountContext.class
                && method.getParameters()[1].getType() == String.class) {

            if (this.registerMethod == null) {
                this.registerMethod = method;
            } else {
                throw new BotMethodFormatException("注册方法只能有一个");
            }
        } else {
            throw new BotMethodFormatException("注册方法错误, " +
                    "应为 Result methodName(AccountContext ac, String inviteCode)");
        }
    }

    /**
     * 登录方法处理器
     *
     * @param method method
     */
    private void loginMethodHandler(Method method) {
        if (method.getReturnType() == Result.class
                && method.getParameterCount() == 1
                && method.getParameters()[0].getType() == AccountContext.class) {

            if (this.loginMethod == null) {
                this.loginMethod = method;
            } else {
                throw new BotMethodFormatException("登录方法只能有一个");
            }
        } else {
            throw new BotMethodFormatException("登录方法错误, " +
                    "应为 Result methodName(AccountContext ac)");
        }
    }

    /**
     * 奖励查询方法处理器
     *
     * @param method       method
     * @param botJobMethod botJobMethod
     * @param jobMap       jobMap
     */
    private void queryRewardMethodHandler(Method method, BotMethod botJobMethod, Map<String, AutoBotJobParam> jobMap) {
        if (method.getReturnType() == Result.class
                && method.getParameterCount() == 1
                && method.getParameters()[0].getType() == AccountContext.class) {

            if (this.updateRewordMethod == null) {
                this.updateRewordMethod = method;
            } else {
                throw new BotMethodFormatException("收益查询方法只能有一个");
            }

            AutoBotJobParam queryRewardJob = buildAutoBotJobParam(method, botJobMethod);

            jobMap.put(BotJobType.QUERY_REWARD.name(), queryRewardJob);
        } else {
            throw new BotMethodFormatException("收益查询方法错误, " +
                    "应为 Result methodName(AccountContext ac)");
        }
    }

    /**
     * 定时任务方法处理器
     *
     * @param method       method
     * @param botJobMethod botJobMethod
     * @param jobMap       jobMap
     */
    private void timedTaskMethodHandler(Method method, BotMethod botJobMethod, Map<String, AutoBotJobParam> jobMap) {
        if (method.getParameterCount() == 1
                && method.getParameters()[0].getType() == AccountContext.class
        ) {

            AutoBotJobParam autoBotJob = buildAutoBotJobParam(method, botJobMethod);
            String jobName = autoBotJob.getJobName();

            if (!jobMap.containsKey(jobName)) {
                jobMap.put(jobName, autoBotJob);
            } else {
                throw new BotMethodFormatException("任务名称重复, " + jobName);
            }
        } else {
            throw new BotMethodFormatException("定时任务方法错误, " +
                    "应为 Result methodName(AccountContext ac)");
        }
    }

    /**
     * Web socket 方法处理器
     *
     * @param method           method
     * @param botJobMethodAnno botJobMethodAnno
     * @param jobMap           jobMap
     */
    private void webSocketConnectMethodHandler(Method method, BotMethod botJobMethodAnno, Map<String, AutoBotJobParam> jobMap) {
        Class<?> returnType = method.getReturnType();

        // 检查方法是否符合要求
        if (BaseBotWSClient.class.isAssignableFrom(returnType)
                && method.getParameterCount() == 1
                && method.getParameters()[0].getType() == AccountContext.class
        ) {
            try {
                // 符合要求，添加到jobMap
                AutoBotJobParam jobParam = buildAutoBotJobParam(WebSocketClientLauncher.lanuchMethod, botJobMethodAnno);

                // 更改执行target，添加额外参数
                jobParam.setTarget(webSocketClientLauncher);
                jobParam.setExtraParams(new Object[]{jobParam, new AccountWSClientBuilder() {
                    @Override
                    public BaseBotWSClient<?, ?> build(AccountContext accountContext) throws InvocationTargetException, IllegalAccessException {
                        Object invoke = method.invoke(getInstance(), accountContext);

                        return (BaseBotWSClient<?, ?>) invoke;
                    }
                }});

                // 添加到jobMap
                jobMap.put(jobParam.getJobName(), jobParam);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new BotMethodFormatException("websocket 方法错误, 应为 BotWebSocketContext<?,?> methodName(AccountContext)");
        }
    }

    /**
     * 根据方法注解创建 AutoBotJob
     *
     * @param method           方法
     * @param botJobMethodAnno 方法上的BotMethod注解
     * @return AutoBotJobParam
     */
    private @NotNull AutoBotJobParam buildAutoBotJobParam(Method method, BotMethod botJobMethodAnno) {
        AutoBotJobParam autoBotJobParam = null;

        try {
            Integer intervalInSecond = null;
            CronExpression cronExpression = null;

            if (botJobMethodAnno.intervalInSecond() != 0) {
                intervalInSecond = botJobMethodAnno.intervalInSecond();
            } else if (StrUtil.isNotBlank(botJobMethodAnno.cronExpression())) {
                cronExpression = new CronExpression(botJobMethodAnno.cronExpression());
            } else {
                throw new IllegalArgumentException("定时任务需设置时间间隔或cron表达式");
            }

            autoBotJobParam = new AutoBotJobParam(
                    this,
                    StrUtil.isBlank(botJobMethodAnno.jobName()) ? method.getName() : botJobMethodAnno.jobName(),
                    botJobMethodAnno.description(),
                    method,
                    cronExpression,
                    intervalInSecond,
                    botJobMethodAnno.concurrentCount(),
                    botJobMethodAnno.bowWsConfig(),
                    null,
                    null
            );
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "[%s]-[%s]BotJobMethod上错误的cron表达式[%s]",
                            getBotInfo().getName(),
                            method.getName(),
                            botJobMethodAnno.cronExpression()
                    ),
                    e
            );
        }

        return autoBotJobParam;
    }

    /**
     * 运行bot method
     *
     * @param method method
     * @param args   args
     * @return CompletableFuture<R>
     */
    private @NotNull CompletableFuture<Result> invokeBotMethod(Method method, Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return (Result) method.invoke(this, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BotMethodInvokeException(String.format(
                        "执行[%s]-[%s]方法发生异常",
                        getBotInfo().getName(),
                        registerMethod.getName()
                ), e);
            }
        }, getExecutorService());
    }


    /**
     * 获取并发控制的信号量
     *
     * @param jobName jobName
     * @return Semaphore
     */
    private Semaphore getCcSemaphore(String jobName) {
        return jobCCSemaphoreMap.computeIfAbsent(jobName, key -> {
            AutoBotJobParam autoBotJobParam = autoBotJobMap.get(key);
            if (autoBotJobParam == null) {
                return new Semaphore(getRequestConcurrentCount());
            }
            return new Semaphore(autoBotJobParam.getConcurrentCount());
        });
    }
}
