package cn.com.helei.application.keitokun;

import cn.com.helei.bot.core.BaseBotWSClient;
import cn.com.helei.bot.core.SimpleBotWSClient;
import cn.com.helei.bot.core.bot.WSTaskAutoBot;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.config.BaseAutoBotConfig;
import cn.com.helei.bot.core.supporter.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.netty.constants.WebsocketClientStatus;
import cn.com.helei.bot.core.supporter.commandMenu.MenuNodeMethod;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class KeitokunWSAutoBot extends WSTaskAutoBot<KeitokunConfig, JSONObject, JSONObject> {

    private final static String TODAY_REMAINING_TAP_KEY = "remaining_tap";

    private final static String TODAY_KEY = "today";

    private final static String TOKEN_KEY = "token";

    private final static String UID_KEY = "uid";

    private final static int noResponsePingLimit = 10;

    private final static Map<BaseBotWSClient<JSONObject, JSONObject>, Integer> clientNoResponsePingCount = new ConcurrentHashMap<>();

    private final static Map<BaseBotWSClient<JSONObject, JSONObject>, Integer> requestIdMap = new ConcurrentHashMap<>();

    private final Random random = new Random();

    private final Semaphore accountSemaphore;

    public KeitokunWSAutoBot(KeitokunConfig config) {
        super(config);
        accountSemaphore = new Semaphore(config.getConcurrentCount());
    }


    @Override
    protected void typedAccountsLoadedHandler(Map<String, List<AccountContext>> typedAccountMap) {
        String today = LocalDate.now().toString();

        String path = getAppConfigDir() + File.separator + getBotConfig().getTokenAndUidFileName();

        // 读取 Excel 数据
        List<KeitokunUidAndTokenDto> tokenDtoList = EasyExcel.read(path)
                .head(KeitokunUidAndTokenDto.class)
                .sheet()
                .doReadSync();

        // KeitokunUidAndTokenDto 按照type分区
        Map<String, List<KeitokunUidAndTokenDto>> typedDto = tokenDtoList.stream().collect(Collectors.groupingBy(dto -> {
            if (dto.getType() == null) return "NULL";
            return dto.getType();
        }));

        // 遍历类型
        typedAccountMap.forEach((type, accounts) -> {
            // 获取该类型的token
            List<KeitokunUidAndTokenDto> tokenDtos = typedDto.get(type);

            if (tokenDtos != null) {
                Map<Integer, AccountContext> accountIdMap = new ConcurrentHashMap<>();
                accounts.forEach(accountContext -> accountIdMap.put(accountContext.getAccountBaseInfo().getId(), accountContext));

                for (KeitokunUidAndTokenDto tokenDto : tokenDtos) {

                    String uid = tokenDto.getUid();
                    String token = tokenDto.getToken();

                    // 筛选可用的
                    if (tokenDto.getId() != null && StrUtil.isNotBlank(uid) && StrUtil.isNotBlank(token)) {
                        // 获取token dto 对应的accountContext
                        AccountContext accountContext = accountIdMap.get(tokenDto.getId());

                        if (accountContext != null) {
                            // 写入token 和 uid
                            accountContext.setParam(TOKEN_KEY, token);
                            accountContext.setParam(UID_KEY, uid);
                        }
                    }
                }
            }
        });
    }


    @Override
    public BaseBotWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {
        String prefix = accountContext.getSimpleInfo();

        // Step 1 检查是否有uid
        String uid = accountContext.getParam("uid");

        if (StrUtil.isBlank(uid)) {
            log.warn("{} uid不可用", prefix);
            return null;
        }

        // Step 2 判断今天的点击是否完成
        String today = LocalDate.now().toString();
        String remainingTapStr = accountContext.getParam(TODAY_REMAINING_TAP_KEY);
        String accountSaveDay = accountContext.getParam(TODAY_KEY);

        // 新的一天
        if (accountSaveDay == null || !accountSaveDay.equals(today) || StrUtil.isBlank(remainingTapStr)) {
            accountContext.setParam(TODAY_KEY, LocalDate.now().toString());
            accountContext.setConnectUrl(getBotConfig().getWsBaseUrl() + "?uid=" + accountContext.getParam(UID_KEY));
            accountContext.setParam(TODAY_REMAINING_TAP_KEY, "500");
        } else if(Integer.parseInt(remainingTapStr) > 0){ // 日内没点击完
            accountContext.setConnectUrl(getBotConfig().getWsBaseUrl() + "?uid=" + accountContext.getParam(UID_KEY));
        } else {
            // 今天点击完的
            log.warn("{} 今日点击已完成", prefix);
            return null;
        }

        log.info("{}-uid[{}] 开始创建ws客户端", prefix, uid);

        // Step 3 创建ws客户端
        SimpleBotWSClient simpleDepinWSClient = new SimpleBotWSClient(this, accountContext);

        requestIdMap.put(simpleDepinWSClient, 0);
        simpleDepinWSClient.setAllIdleTimeSecond(getBotConfig().getAutoClaimIntervalSeconds() * 2);

        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("Origin", "https://game.keitokun.com");
        headers.put("Host", "game.keitokun.com");
        headers.put("cookie", "_ga=;_ga_LD12CVNEZ7=");

        headers.forEach(httpHeaders::add);

        simpleDepinWSClient.setHeaders(httpHeaders);

        return simpleDepinWSClient;
    }

    @Override
    public void whenAccountClientStatusChange(BaseBotWSClient<JSONObject, JSONObject> depinWSClient, WebsocketClientStatus clientStatus) {
        AccountContext accountContext = depinWSClient.getAccountContext();

        String printPrefix = accountContext.getSimpleInfo() + "-" + accountContext.getParam(UID_KEY);

        switch (clientStatus) {
            case RUNNING -> {
                log.info("{} 连接ws服务器[{}]成功", printPrefix, accountContext.getConnectUrl());

            }
            case STOP -> {
                log.info("{} 连接到ws服务器[{}]失败", printPrefix, accountContext.getConnectUrl());

                accountContext.getConnectStatusInfo().getRestart().incrementAndGet();
            }
            case SHUTDOWN -> {
                log.info("{} ws连接已断开", printPrefix);
                requestIdMap.remove(depinWSClient);
            }
        }
    }

    @Override
    public void whenAccountReceiveResponse(BaseBotWSClient<JSONObject, JSONObject> depinWSClient, Object id, JSONObject response) {
        Integer cmd = response.getInteger("cmd");
        JSONObject data = response.getJSONObject("data");

        AccountContext accountContext = depinWSClient.getAccountContext();
        ConnectStatusInfo connectStatusInfo = accountContext.getConnectStatusInfo();

        String prefix = accountContext.getSimpleInfo() + "-" + accountContext.getParam("uid");

        log.info("{} 收到消息 {}", prefix, response);
        switch (cmd) {
            case 1001:
                Integer totalNum = data.getInteger("totalNum");
                Integer collectNum = data.getInteger("collectNum");

                accountContext.setParam(TODAY_REMAINING_TAP_KEY,
                        String.valueOf(Math.max(0, totalNum - collectNum)));

                // 今日的领完了, 关闭
                if (totalNum <= collectNum) {
                    log.info("{} 今日keitokun点击已完成，断开ws连接", prefix);
                    removeAccountTimer(accountContext);

                    accountContext.setParam(TODAY_KEY, LocalDate.now().toString());
                    depinWSClient.shutdown();
                }

                log.info("{} 收到响应,[{}/{}](已点击/剩余)", prefix,
                        collectNum, totalNum);

                accountContext.getRewordInfo().setTotalPoints(data.getInteger("keitoAmount") * 1.0);
                accountContext.getRewordInfo().setTodayPoints(collectNum * 1.0);

                connectStatusInfo.getHeartBeat().incrementAndGet();
                break;
            default:
                log.warn("{} 收到未知响应[{}]", prefix, response);
                connectStatusInfo.getErrorHeartBeat().incrementAndGet();
        }

        connectStatusInfo.setUpdateDateTime(LocalDateTime.now());
    }

    @Override
    public void whenAccountReceiveMessage(BaseBotWSClient<JSONObject, JSONObject> depinWSClient, JSONObject message) {

    }

    @Override
    public JSONObject getHeartbeatMessage(BaseBotWSClient<JSONObject, JSONObject> depinWSClient) {
        AccountContext accountContext = depinWSClient.getAccountContext();

        Integer count = clientNoResponsePingCount.compute(depinWSClient, (k, v) -> {
            if (v != null && v >= noResponsePingLimit) {
                return null;
            }
            return v == null ? 1 : v + 1;
        });

        if (count == null) {
            log.warn("{} 长时间未收到pong，关闭客户端", depinWSClient.getAccountContext().getSimpleInfo());
            depinWSClient.close();
            return null;
        }


        JSONObject frame = new JSONObject();
        frame.put("cmd", 1001);
        frame.put("id", requestIdMap.compute(depinWSClient, (k, v) -> v == null ? 1 : v + 1));
        frame.put("uid", accountContext.getParam("uid"));

        JSONObject data = new JSONObject();
        int randomClickTimes = getRandomClickTimes();

        data.put("amount", randomClickTimes);
        data.put("collectNum", randomClickTimes);
        data.put("timestamp", System.currentTimeMillis());
        frame.put("data", data);

        log.info("[{}] 发送心跳[{}]", accountContext.getSimpleInfo(), frame);
        accountContext.getConnectStatusInfo().getHeartBeat().getAndIncrement();

        return frame;
    }

    @Override
    public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return null;
    }

    @Override
    public CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> verifierAccountEmail(AccountContext accountContext, Message message) {
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
    @MenuNodeMethod(title = "自动完成任务", description = "开始自动完成任务")
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

        String printStr = String.format("账户[%s]-uid[%s]-proxy[%s]",
                accountContext.getName(), uid, accountContext.getProxy().getAddressStr());

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

        String printStr = String.format("账户[%s]-uid[%s]-proxy[%s]",
                accountContext.getName(), uid, accountContext.getProxy().getAddressStr());

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
        KeitokunWSAutoBot mKeitokunWSDepinBot = new KeitokunWSAutoBot(KeitokunConfig.loadYamlConfig("keitokun/keitokun.yaml"));

        MenuCMDLineAutoBot<BaseAutoBotConfig> menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(mKeitokunWSDepinBot, List.of(DefaultMenuType.START_ACCOUNT_CLAIM));

        menuCMDLineAutoBot.start();
    }
}
