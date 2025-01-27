package cn.com.helei.DepinBot.app.keitokun;

import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.SimpleDepinWSClient;
import cn.com.helei.DepinBot.core.bot.WSMenuCMDLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.commandMenu.DefaultMenuType;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.dto.account.ConnectStatusInfo;
import cn.com.helei.DepinBot.core.exception.DepinBotInitException;
import cn.com.helei.DepinBot.core.netty.constants.WebsocketClientStatus;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KeitokunWSDepinBot extends WSMenuCMDLineDepinBot<KeitokunConfig, JSONObject, JSONObject> {

    private final static String TODAY_REMAINING_TAP_KEY = "remaining_tap";

    private final static String TODAY_KEY = "today";

    private final static Map<BaseDepinWSClient<JSONObject, JSONObject>, Integer> requestIdMap = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public KeitokunWSDepinBot(KeitokunConfig config) {
        super(config);
    }

    @Override
    protected void doInit() throws DepinBotInitException {
        super.doInit();

        // 将tokens加载到对应的accountContext
        List<AccountContext> accounts = getAccounts();

        List<String> uids = getBotConfig().getUids();
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
                    accountContext.setConnectUrl(getBotConfig().getWsBaseUrl() + "?uid=" + uid);
                } else {
                    accountContext.setParam("uid", null);
                }
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

                                // 发送心跳
                                JSONObject heartbeatMessage = getHeartbeatMessage(depinWSClient);
                                log.info("[{}] 发送心跳[{}]", printPrefix, heartbeatMessage);

                                depinWSClient.sendMessage(heartbeatMessage);
                                accountContext.getConnectStatusInfo().getHeartBeat().getAndIncrement();

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
                if (totalNum < collectNum) {
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
        getAccounts().forEach(account -> {


        });

        return null;
    }

    public CompletableFuture<List<String>> queryAccountUnClaimedTask(AccountContext accountContext) {
//        String url = "https://game.keitokun.com/api/v1/quest/getStatesList";
//        String uid = accountContext.getParam("uid");
//
//        JSONObject params = new JSONObject();
//        params.put("uid", uid);
//
//        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
//
//        return syncRequest(
//                accountContext.getProxy(),
//            url,
//            "get",
//
//        )
        return null;
    }
}
