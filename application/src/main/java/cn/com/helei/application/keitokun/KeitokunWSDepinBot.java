package cn.com.helei.application.keitokun;

import cn.com.helei.bot.core.BaseDepinWSClient;
import cn.com.helei.bot.core.SimpleDepinWSClient;
import cn.com.helei.bot.core.bot.WSMenuCMDLineDepinBot;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.exception.DepinBotInitException;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.netty.constants.WebsocketClientStatus;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

@Slf4j
public class KeitokunWSDepinBot extends WSMenuCMDLineDepinBot<KeitokunConfig, JSONObject, JSONObject> {

    private final static String TODAY_REMAINING_TAP_KEY = "remaining_tap";

    private final static String TODAY_KEY = "today";

    private final static String TOKEN_KEY = "token";

    private final static Map<BaseDepinWSClient<JSONObject, JSONObject>, Integer> requestIdMap = new ConcurrentHashMap<>();

    private final static Map<BaseDepinWSClient<JSONObject, JSONObject>, Integer> clientNoResponseHeartbeatMap = new ConcurrentHashMap<>();

    private final Random random = new Random();

    private final Semaphore accountSemaphore;

    public KeitokunWSDepinBot(KeitokunConfig config) {
        super(config);
        accountSemaphore = new Semaphore(config.getConcurrentCount());
    }

    @Override
    protected void doInit() throws DepinBotInitException {
        super.doInit();

        // 将tokens加载到对应的accountContext
        List<AccountContext> accounts = getAccounts();

        List<String> uids = getBotConfig().getUids();
        List<String> tokens = getBotConfig().getTokens();
        if (uids == null) return;

        String today = LocalDate.now().toString();

        for (int i = 0; i < uids.size(); i++) {
            if (i < accounts.size()) {
                String uid = uids.get(i);
                AccountContext accountContext = accounts.get(i);

                String remainingTapStr = accountContext.getParam(TODAY_REMAINING_TAP_KEY);
                String accountSaveDay = accountContext.getParam(TODAY_KEY);

                // 今天还有没点击的,或者就没点击
                if (accountSaveDay == null || remainingTapStr == null
                        || !accountSaveDay.equals(today) || Integer.parseInt(remainingTapStr) >= 0
                ) {
                    accountContext.setParam("uid", uid);
                    accountContext.setParam(TODAY_KEY, LocalDate.now().toString());
                    accountContext.setConnectUrl(getBotConfig().getWsBaseUrl() + "?uid=" + uid);
                } else {
                    accountContext.setParam("uid", null);
                }

                accountContext.setParam(TOKEN_KEY, tokens.get(i));
            }
        }
    }

    @Override
    public BaseDepinWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {
        String uid = accountContext.getParam("uid");

        if (uid == null || "-1".equals(uid)) {
            return null;
        }

        SimpleDepinWSClient simpleDepinWSClient = new SimpleDepinWSClient(this, accountContext);

        requestIdMap.put(simpleDepinWSClient, 0);
        simpleDepinWSClient.setAllIdleTimeSecond(getBotConfig().getAutoClaimIntervalSeconds() * 2);

        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.forEach(httpHeaders::set);
        httpHeaders.set("origin", "https://game.keitokun.com");
        httpHeaders.set("host", "game.keitokun.com");
        httpHeaders.set("Upgrade", "websocket");
        httpHeaders.set("connection", "Upgrade");
        simpleDepinWSClient.setHeaders(httpHeaders);

        return simpleDepinWSClient;
    }

    @Override
    public void whenAccountClientStatusChange(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, WebsocketClientStatus clientStatus) {
        AccountContext accountContext = depinWSClient.getAccountContext();

        String printPrefix = String.format("账户[%s]-uid[%s]-proxy[%s]",
                accountContext.getName(), accountContext.getParam("uid"), accountContext.getProxy().getAddressStr());

        switch (clientStatus) {
            case RUNNING -> {
                log.info("[{}] 连接ws服务器[{}]成功", printPrefix, accountContext.getConnectUrl());

                // 随机时间发送心跳
                addTimer(() -> {
                            try {
                                TimeUnit.MILLISECONDS.sleep(random.nextLong(5000) + 500);

                                Integer noResponseHeartBeatCount = clientNoResponseHeartbeatMap.get(depinWSClient);

                                // 未收到响应的心跳数超过限制
                                if (noResponseHeartBeatCount != null && noResponseHeartBeatCount > 10) {
                                    depinWSClient.close();
                                    clientNoResponseHeartbeatMap.remove(depinWSClient);
                                    return false;
                                }


                                // 发送心跳
                                JSONObject heartbeatMessage = getHeartbeatMessage(depinWSClient);
                                log.info("[{}] 发送心跳[{}]", printPrefix, heartbeatMessage);

                                depinWSClient.sendMessage(heartbeatMessage);
                                accountContext.getConnectStatusInfo().getHeartBeat().getAndIncrement();

                                //设置未响应心跳数
                                clientNoResponseHeartbeatMap.compute(depinWSClient, (k, v) -> v == null ? 1 : v + 1);
                            } catch (Exception e) {
                                log.error("[{}] 发送心跳错误, {}", printPrefix, e.getMessage());
                            }

                            return true;
                        },
                        getBotConfig().getAutoClaimIntervalSeconds() + random.nextInt(8),
                        TimeUnit.SECONDS,
                        accountContext
                );
            }
            case STOP -> {
                log.info("[{}] 连接到ws服务器[{}]失败", printPrefix, accountContext.getConnectUrl());

                accountContext.getConnectStatusInfo().getRestart().incrementAndGet();
            }
            case SHUTDOWN -> {
                log.info("[{}] ws连接已断开", printPrefix);
                requestIdMap.remove(depinWSClient);
            }
        }
    }

    @Override
    public void whenAccountReceiveResponse(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, Object id, JSONObject response) {
        Integer cmd = response.getInteger("cmd");
        JSONObject data = response.getJSONObject("data");

        AccountContext accountContext = depinWSClient.getAccountContext();
        ConnectStatusInfo connectStatusInfo = accountContext.getConnectStatusInfo();

        switch (cmd) {
            case 1001:
                clientNoResponseHeartbeatMap.remove(depinWSClient);

                Integer totalNum = data.getInteger("totalNum");
                Integer collectNum = data.getInteger("collectNum");
                log.info("[{}]收到响应,[{}/{}](已点击/剩余)", accountContext.getParam("uid"),
                        collectNum, totalNum);

                accountContext.getRewordInfo().setTotalPoints(data.getInteger("keitoAmount") * 1.0);
                accountContext.getRewordInfo().setTodayPoints(collectNum * 1.0);

                connectStatusInfo.getHeartBeat().incrementAndGet();

                accountContext.setParam(TODAY_REMAINING_TAP_KEY,
                        String.valueOf(Math.max(0, totalNum - collectNum)));

                // 今日的领完了, 关闭
                if (totalNum <= collectNum) {
                    log.info("账户[{}]今日keitokun点击已完成，断开ws连接", accountContext.getParam("uid"));

                    depinWSClient.shutdown();
                    removeAccountTimer(accountContext);

                    accountContext.setParam(TODAY_KEY, LocalDate.now().toString());
                }
                break;
            default:
                log.warn("[{}]收到未知响应[{}]", accountContext.getName(), response);
                connectStatusInfo.getErrorHeartBeat().incrementAndGet();
        }

        connectStatusInfo.setUpdateDateTime(LocalDateTime.now());
    }

    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, JSONObject message) {

    }

    @Override
    public JSONObject getHeartbeatMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient) {
        JSONObject frame = new JSONObject();
        frame.put("cmd", 1001);
        frame.put("id", requestIdMap.compute(depinWSClient, (k, v) -> v == null ? 1 : v + 1));
        frame.put("uid", depinWSClient.getAccountContext().getParam("uid"));

        JSONObject data = new JSONObject();
        int randomClickTimes = getRandomClickTimes();
        data.put("amount", randomClickTimes);
        data.put("collectNum", randomClickTimes);
        data.put("timestamp", System.currentTimeMillis());
        frame.put("data", data);

        return frame;
    }


    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {
        defaultMenuTypes.add(DefaultMenuType.START_ACCOUNT_CLAIM);

        mainMenu.addSubMenu(new CommandMenuNode("自动完成任务", "开始自动完成任务", this::autoClaimTask));
    }


    @Override
    protected CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return null;
    }

    @Override
    protected CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return null;
    }

    private int getRandomClickTimes() {
        return random.nextInt(5) + 1;
    }


    /**
     * 自动完成任务
     *
     * @return String
     */
    private String autoClaimTask() {
        // Step 1 遍历账户
        getAccounts().forEach(account -> {
            if (account.getParam("uid") == null
                    || account.getParam("token") == null
            ) {
                return;
            }

            try {
                accountSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Step 2 查询未完成任务
            queryAccountUnClaimedTask(account)
                    .thenAcceptAsync(unClaimedList -> {
                        if (unClaimedList != null && !unClaimedList.isEmpty()) {
                            // Step 3 领取
                            List<CompletableFuture<Boolean>> claimFutures = unClaimedList
                                    .stream()
                                    .map(kType -> claimTask(account, kType))
                                    .toList();

                            for (CompletableFuture<Boolean> claimFuture : claimFutures) {
                                try {
                                    claimFuture.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    log.error("等待完成任务发生异常，{}", e.getMessage());
                                }
                            }
                        }
                    }, getExecutorService())
                    .whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            log.error("字段领取时发生异常", throwable);
                        }
                        accountSemaphore.release();
                    });
        });

        return null;
    }


    /**
     * 查询未完成的任务
     *
     * @param accountContext accountContext
     * @return CompletableFuture<List < String>>
     */
    public CompletableFuture<List<String>> queryAccountUnClaimedTask(AccountContext accountContext) {
        String url = "https://game.keitokun.com/api/v1/quest/getStatesList";
        String uid = accountContext.getParam("uid");
        String token = accountContext.getParam("token");

        JSONObject params = new JSONObject();
        params.put("uid", uid);

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("x-token", token);
        headers.put("timestamp", String.valueOf(System.currentTimeMillis()));

        String printStr = String.format("账户[%s]-uid[%s]-token[%s]",
                accountContext.getName(), uid, token.substring(0, Math.min(16, token.length() - 1)));

        return syncRequest(
                accountContext.getProxy(),
                url,
                "get",
                headers,
                params,
                null,
                () -> printStr + " 开始查询任务列表"
        ).thenApplyAsync(responseStr -> {
            JSONObject response = JSONObject.parseObject(responseStr);
            if (response.getInteger("code") == 0) {
                JSONArray data = response.getJSONArray("data");
                List<String> unclaimedKTypeList = new ArrayList<>();

                for (int i = 0; i < data.size(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    if ("enable".equals(item.getString("status"))
                            && "unclaim".equals(item.getString("states"))) {
                        unclaimedKTypeList.add(item.getString("kType"));
                    }
                }

                log.info("{} 查询任务列表成功，共[{}]个未完成任务", printStr, unclaimedKTypeList.size());

                return unclaimedKTypeList;
            } else {
                log.error("{} 查询任务列表失败, {}", printStr, response);
                return null;
            }
        });
    }

    private CompletableFuture<Boolean> claimTask(AccountContext accountContext, String kType) {
        String url = "https://game.keitokun.com/api/v1/quest/claim";
        String uid = accountContext.getParam("uid");
        String token = accountContext.getParam("token");

        JSONObject body = new JSONObject();
        body.put("uid", uid);
        body.put("kType", kType);

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("x-token", token);
        headers.put("timestamp", String.valueOf(System.currentTimeMillis()));

        String printStr = String.format("账户[%s]-uid[%s]-token[%s]",
                accountContext.getName(), uid, token);

        return syncRequest(
                accountContext.getProxy(),
                url,
                "post",
                headers,
                null,
                body,
                () -> printStr + " 正在领取 [" + kType + "] 任务"
        ).thenApplyAsync(responseStr -> {
            try {
                JSONObject response = JSONObject.parseObject(responseStr);
                if (response.getInteger("code") == 0) {
                    log.info("{} 领取[{}]任务奖励成功", printStr, kType);
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException(printStr + " 领取[" + kType + "]任务发生异常", e);
            }

            return false;
        });
    }

    public static void main(String[] args) throws DepinBotStartException {
        KeitokunWSDepinBot mKeitokunWSDepinBot = new KeitokunWSDepinBot(KeitokunConfig.loadYamlConfig("keitokun.yaml"));

        mKeitokunWSDepinBot.init();
        mKeitokunWSDepinBot.start();
    }
}
