package cn.com.helei.DepinBot.openLedger;

import cn.com.helei.DepinBot.core.AbstractDepinWSClient;
import cn.com.helei.DepinBot.core.CommandLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.dto.RewordInfo;
import cn.com.helei.DepinBot.core.util.RestApiClient;
import cn.com.helei.DepinBot.core.util.RestApiClientFactory;
import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * OpenLedger depin 项目机器人
 */
@Slf4j
public class OpenLedgerDepinBot extends CommandLineDepinBot<String, String> {

    private final OpenLedgerConfig openLedgerConfig;

    public OpenLedgerDepinBot(OpenLedgerConfig openLedgerConfig) {
        super(openLedgerConfig);
        this.openLedgerConfig = openLedgerConfig;
    }

    @Override
    protected CommandMenuNode buildMenuNode() {
        CommandMenuNode menuNode = new CommandMenuNode("主菜单", "这是主菜单，请选择", null);
        CommandMenuNode menuNodeA = new CommandMenuNode("子菜单A", "这是子菜单A，请选择", null);
        CommandMenuNode menuNodeB = new CommandMenuNode("子菜单B", "这是子菜单B，请选择", null);

        menuNodeA.addSubMenu(new CommandMenuNode("子菜单A-1", "这是子菜单A-1，请选择", () -> "haha进入了A-1"));
        menuNodeA.addSubMenu(new CommandMenuNode("子菜单A-2", "这是子菜单A-2，请选择", () -> "haha进入了A-2"));

        menuNodeB.addSubMenu(new CommandMenuNode("子菜单B-1", "这是子菜单B-1，请选择", null));
        menuNodeB.addSubMenu(new CommandMenuNode("子菜单B-2", "这是子菜单B-2，请选择", null));

        menuNode.addSubMenu(menuNodeA);
        menuNode.addSubMenu(menuNodeB);

        return menuNode;
    }

    @Override
    public AbstractDepinWSClient<String, String> buildAccountWSClient(AccountContext accountContext) {
        return new OpenLedgerDepinWSClient(accountContext);
    }

    @Override
    public void whenAccountConnected(AccountContext accountContext, Boolean success) {

        if (BooleanUtil.isTrue(success)) {
            //Step 1 1 设置定时刷新奖励信息设置
            addTimer(() -> {
                updateRewardInfo(accountContext);
            }, openLedgerConfig.getAccountRewardRefreshIntervalSeconds(), TimeUnit.SECONDS);
        }
    }

    /**
     * 更新奖励信息
     *
     * @param accountContext accountContext
     */
    private void updateRewardInfo(AccountContext accountContext) {
        log.info("开始账户[{}]更新奖励信息", accountContext.getClientAccount().getName());

        RewordInfo rewordInfo = accountContext.getRewordInfo();
        if (rewordInfo == null) {
            return;
        }

        //计算当前获得的奖励
        CompletableFuture<JSONObject> rewardFuture = queryReward(accountContext);
        CompletableFuture<JSONObject> rewardRealtimeFuture = queryRewardRealtime(accountContext);
        CompletableFuture<JSONObject> claimDetailFuture = queryClaimDetail(accountContext);

        rewardFuture.thenAcceptBothAsync(rewardRealtimeFuture, (reward, rewardRealtime) -> {
                    //总分
                    rewordInfo.addTotalPoints(reward.getDouble("totalPoint"));
                    rewordInfo.addSessionPoints(reward.getDouble("point"));
                    rewordInfo.setSessionId(reward.getString("name"));


                    //今日链接奖励
                    if (rewardRealtime != null) {
                        Double todayHeartBeats = rewardRealtime.getDouble("total_heartbeats");

                        rewordInfo.addTodayPoints(todayHeartBeats);
                        rewordInfo.addTotalPoints(todayHeartBeats);
                        rewordInfo.addSessionPoints(todayHeartBeats);
                    }

                    //今日签到奖励
                    try {
                        JSONObject claimDetail = claimDetailFuture.get();
                        if (claimDetail != null && claimDetail.getBoolean("claimed")) {
                            Double dailyPoint = claimDetail.getDouble("dailyPoint");

                            rewordInfo.addTodayPoints(dailyPoint);
                            rewordInfo.addTotalPoints(dailyPoint);
                            rewordInfo.addSessionPoints(dailyPoint);
                        }
                    } catch (Exception e) {
                        log.error("计算奖励查询每日签到信息发生异常, [{}}", accountContext, e);
                    }

                    rewordInfo.setUpdateTime(LocalDateTime.now());
                    log.info("账户[{}]更新奖励信息更新完毕[{}}", accountContext.getClientAccount().getName(), rewordInfo);
                }, getExecutorService())
                .exceptionally(throwable -> {
                    log.error("计算奖励查询每日签到信息发生异常, [{}}", accountContext, throwable);
                    return null;
                });
    }


    /**
     * 查询每日签到信息
     *
     * @param accountContext accountContext
     * @return {
     * "status": "SUCCESS",
     * "message": null,
     * "data": {
     * "tier": "Shrimp",
     * "image": "<a href="https://cdn.openledger.xyz/Tier-2-active.png">...</a>",
     * "claimed": true,
     * "dailyPoint": 50,
     * "nextClaim": "2025-01-19T00:00:00.000Z"
     * }
     * }
     */
    private CompletableFuture<JSONObject> queryClaimDetail(AccountContext accountContext) {
        RestApiClient restApiClient = RestApiClientFactory.getClient(accountContext.getProxy());
        String url = "https://rewardstn.openledger.xyz/api/v1/claim_details";

        return requestAndTakeData(accountContext, restApiClient, url);
    }

    /**
     * 查查reword
     *
     * @param accountContext accountContext
     * @return {
     * "status": "SUCCESS",
     * "message": null,
     * "data": {
     * "totalPoint": "2989.00",
     * "point": "2989.00",
     * "name": "Epoch 1",
     * "endDate": "2025-01-31"
     * }
     * }
     */
    private CompletableFuture<JSONObject> queryReward(AccountContext accountContext) {
        RestApiClient restApiClient = RestApiClientFactory.getClient(accountContext.getProxy());
        String url = "https://rewardstn.openledger.xyz/api/v1/reward";

        return requestAndTakeData(accountContext, restApiClient, url);
    }

    /**
     * 查查reword history
     *
     * @param accountContext accountContext
     * @return {
     * "status": "SUCCESS",
     * "message": null,
     * "data": [
     * {
     * "date": "2025-01-18",
     * "total_points": 50,
     * "details": [
     * {
     * "claim_type": 2,
     * "points": 50
     * }
     * ]
     * }
     * ]
     * }
     */
    private CompletableFuture<JSONObject> queryRewordHistory(AccountContext accountContext) {
        RestApiClient restApiClient = RestApiClientFactory.getClient(accountContext.getProxy());
        String url = "https://rewardstn.openledger.xyz/api/v1/reward_history";

        return requestAndTakeData(accountContext, restApiClient, url);
    }


    /**
     * 查查reword realtime
     *
     * @param accountContext accountContext
     * @return {
     * "status": "SUCCESS",
     * "message": null,
     * "data": [
     * {
     * "date": "2025-01-18",
     * "total_heartbeats": "134",
     * "total_scraps": "0",
     * "total_prompts": "0"
     * }
     * ]
     * }
     */
    private CompletableFuture<JSONObject> queryRewardRealtime(AccountContext accountContext) {
        RestApiClient restApiClient = RestApiClientFactory.getClient(accountContext.getProxy());
        String url = "https://rewardstn.openledger.xyz/api/v1/reward_realtime";

        return requestAndTakeData(accountContext, restApiClient, url);
    }

    private CompletableFuture<JSONObject> requestAndTakeData(AccountContext accountContext, RestApiClient restApiClient, String url) {
        return restApiClient
                .request(
                        url,
                        "get",
                        accountContext.getRestHeaders(),
                        null,
                        null
                )
                .thenApplyAsync(s -> {
                    JSONObject resp = JSONObject.parseObject(s);
                    if (resp.getString("status").equalsIgnoreCase("success")) {
                        //一次heartbeats一分
                        return resp.getJSONArray("data").getJSONObject(0);
                    }
                    return null;
                }, getExecutorService());
    }
}
