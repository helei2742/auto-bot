package cn.com.helei.bot.core;

import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.anno.BotWSMethodConfig;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.netty.BaseBotWSClient;
import cn.com.helei.bot.core.supporter.netty.BotJsonWSClient;
import cn.com.helei.bot.core.supporter.netty.constants.WebsocketClientStatus;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KeiToKunBot extends AnnoDriveAutoBot<KeiToKunBot> {

    private static final String UID_KEY = "uid";

    private static final String TODAY_KEY = "today";

    private static final String TODAY_REMAINING_TAP_KEY = "today_remaining_tap";

    private static final String TAP_BASE_URL_KEY = "tap_base_url";

    private final static int noResponsePingLimit = 10;

    private final static Map<BaseBotWSClient<JSONObject, JSONObject>, Integer> clientNoResponsePingCount = new ConcurrentHashMap<>();

    private final static Map<BaseBotWSClient<JSONObject, JSONObject>, Integer> requestIdMap = new ConcurrentHashMap<>();

    private final Random random = new Random();


    public KeiToKunBot(AutoBotConfig autoBotConfig, BotApi botApi) {
        super(autoBotConfig, botApi);
    }


    @Override
    protected KeiToKunBot getInstance() {
        return this;
    }


    @Override
    protected void accountsLoadedHandler(List<AccountContext> accountContexts) {
        String today = LocalDate.now().toString();

        accountContexts.forEach(accountContext -> {
            // 判断今日的有没有点击完成
            String remainingTapStr = accountContext.getParam(TODAY_REMAINING_TAP_KEY);
            String accountSaveDay = accountContext.getParam(TODAY_KEY);

            // 今天还有没点击的,或者就没点击
            if (accountSaveDay == null || remainingTapStr == null
                    || !accountSaveDay.equals(today) || Integer.parseInt(remainingTapStr) >= 0
            ) {
                accountContext.setParam(TODAY_KEY, LocalDate.now().toString());
                accountContext.setParam(TODAY_REMAINING_TAP_KEY, StrUtil.isBlank(remainingTapStr) ? "500" : remainingTapStr);
            } else {
                accountContext.setParam(TODAY_REMAINING_TAP_KEY, "0");
            }
        });
    }


    @BotMethod(
            jobType = BotJobType.WEB_SOCKET_CONNECT,
            jobName = "ws-keep-alive-task",
            cronExpression = "*/10 * * * * ?",
            bowWsConfig = @BotWSMethodConfig(
                    isRefreshWSConnection = true
            )
    )
    public BotJsonWSClient tapConnection(AccountContext accountContext) {
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
            accountContext.setParam(TODAY_REMAINING_TAP_KEY, "500");
        } else if (Integer.parseInt(remainingTapStr) > 0) { // 日内没点击完
            log.info("{} 没点击完，剩余: {}", prefix, remainingTapStr);
        } else {
            // 今天点击完的
            log.warn("{} 今日点击已完成", prefix);
            return null;
        }

        log.info("{}-uid[{}] 开始创建ws客户端", prefix, uid);

        String connectUrl = getAutoBotConfig().getConfig(TAP_BASE_URL_KEY) + "?uid=" + accountContext.getParam(UID_KEY);

        return new BotJsonWSClient(
                accountContext,
                connectUrl
        ) {

            @Override
            public void whenAccountClientStatusChange(WebsocketClientStatus clientStatus) {
                AccountContext accountContext = this.getAccountContext();

                String printPrefix = accountContext.getSimpleInfo() + "-" + accountContext.getParam(UID_KEY);

                switch (clientStatus) {
                    case RUNNING -> {
                        log.info("{} 连接ws服务器[{}]成功", printPrefix, connectUrl);

                    }
                    case STOP -> {
                        log.info("{} 连接到ws服务器[{}]失败", printPrefix, connectUrl);

                        accountContext.getConnectStatusInfo().getRestart().incrementAndGet();
                    }
                    case SHUTDOWN -> {
                        log.info("{} ws连接已断开", printPrefix);
                        requestIdMap.remove(this);
                    }
                }
            }

            @Override
            public JSONObject getHeartbeatMessage() {
                Integer count = clientNoResponsePingCount.compute(this, (k, v) -> {
                    if (v != null && v >= noResponsePingLimit) {
                        return null;
                    }
                    return v == null ? 1 : v + 1;
                });

                if (count == null) {
                    log.warn("{} 长时间未收到pong，关闭客户端", this.getAccountContext().getSimpleInfo());
                    this.close();
                    return null;
                }


                JSONObject frame = new JSONObject();
                frame.put("cmd", 1001);
                frame.put("id", requestIdMap.compute(this, (k, v) -> v == null ? 1 : v + 1));
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
            public void whenAccountReceiveResponse(
                    Object id,
                    JSONObject response
            ) {
                Integer cmd = response.getInteger("cmd");
                JSONObject data = response.getJSONObject("data");

                AccountContext accountContext = this.getAccountContext();
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
                            accountContext.setParam(TODAY_KEY, LocalDate.now().toString());
                            this.shutdown();
                        }

                        log.info("{} 收到响应,[{}/{}](已点击/剩余)", prefix,
                                collectNum, totalNum);

                        accountContext.getRewordInfo().setTotalPoints(data.getInteger("keitoAmount") * 1.0);
                        accountContext.getRewordInfo().setTotalPoints(collectNum * 1.0);

                        connectStatusInfo.getHeartBeat().incrementAndGet();
                        break;
                    default:
                        log.warn("{} 收到未知响应[{}]", prefix, response);
                        connectStatusInfo.getErrorHeartBeat().incrementAndGet();
                }

                connectStatusInfo.setUpdateDateTime(LocalDateTime.now());
            }

            @Override
            public void whenAccountReceiveMessage(
                    JSONObject message
            ) {

            }
        };
    }

    private int getRandomClickTimes() {
        return random.nextInt(5) + 1;
    }

}
