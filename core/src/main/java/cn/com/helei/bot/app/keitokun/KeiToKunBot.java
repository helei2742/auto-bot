package cn.com.helei.bot.app.keitokun;

import cn.com.helei.bot.core.bot.anno.BotApplication;
import cn.com.helei.bot.core.bot.anno.BotMethod;
import cn.com.helei.bot.core.bot.anno.BotWSMethodConfig;
import cn.com.helei.bot.core.bot.base.AnnoDriveAutoBot;
import cn.com.helei.bot.core.bot.constants.BotJobType;
import cn.com.helei.bot.core.dto.config.AutoBotConfig;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.supporter.botapi.BotApi;
import cn.com.helei.bot.core.supporter.netty.BotJsonWSClient;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@BotApplication(name = "keitokun_bot")
public class KeiToKunBot extends AnnoDriveAutoBot<KeiToKunBot> {

    public static final String UID_KEY = "uid";

    public static final String TODAY_KEY = "today";

    public static final String TODAY_REMAINING_TAP_KEY = "today_remaining_tap";

    private static final String TAP_BASE_URL = "wss://game.keitokun.com/api/v1/ws";


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
                accountContext.setParam(TODAY_REMAINING_TAP_KEY,
                        StrUtil.isBlank(remainingTapStr) ? "500" : remainingTapStr);
            } else {
                accountContext.setParam(TODAY_REMAINING_TAP_KEY, "0");
            }
        });
    }


    @BotMethod(
            jobType = BotJobType.WEB_SOCKET_CONNECT,
            jobName = "ws-keep-alive-task",
            bowWsConfig = @BotWSMethodConfig(
                    isRefreshWSConnection = true,
                    heartBeatIntervalSecond = 5,
                    wsConnectCount = 10
            ),
            intervalInSecond = 60 * 60 * 12
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
        if (accountSaveDay == null || !accountSaveDay.equals(today)
                || StrUtil.isBlank(remainingTapStr)) {
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

        String connectUrl = TAP_BASE_URL + "?uid=" + accountContext.getParam(UID_KEY);

        return new KeiToKunWSClient(accountContext, connectUrl);
    }
}
