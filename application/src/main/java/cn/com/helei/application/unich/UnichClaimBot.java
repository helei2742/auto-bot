package cn.com.helei.application.unich;

import cn.com.helei.bot.core.bot.RestTaskAutoBot;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.config.BaseDepinBotConfig;
import cn.com.helei.bot.core.dto.account.AccountBaseInfo;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class UnichClaimBot extends RestTaskAutoBot {

    private static final String TOKEN_KEY = "token";

    public UnichClaimBot(UnichConfig botConfig) {
        super(botConfig);
    }


    @Override
    protected void typedAccountsLoadedHandler(Map<String, List<AccountContext>> typedAccountMap) {
        Map<String, List<String>> tokensMap = ((UnichConfig) getBaseDepinBotConfig()).getTokens();

        typedAccountMap.forEach((type, accountContextList) -> {
            List<String> tokens = tokensMap.get(type);

            if (tokens != null) {
                for (int i = 0; i < Math.min(tokens.size(), accountContextList.size()); i++) {
                    accountContextList.get(i).setParam(TOKEN_KEY, tokens.get(i));
                }
            }

            AccountContext accountContext = loadMainAccount();

            accountContextList.add(accountContext);
        });
    }

    @NotNull
    private AccountContext loadMainAccount() {
        AccountContext accountContext = new AccountContext();
        accountContext.setParam("token", "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI2NzkyMzA5M2ZkNWFjM2E2YWQ3M2VjOTMiLCJzaWduZWRBdCI6MTczNzczNDM0MTAzMiwiaWQiOiI0U1FLVE1ZTlUyQ1VDSDBTIiwidHlwZSI6ImFjY2Vzc1Rva2VuIn0.8Buae8mnHJ0Ur3eUPdaHXCwCjNbOc-a90xvJKSSZ-DI");
        accountContext.setProxy(getStaticProxyPool().getLessUsedItem(1).getFirst());
        accountContext.setBrowserEnv(getBrowserEnvPool().getLessUsedItem(1).getFirst());
        AccountBaseInfo accountBaseInfo = new AccountBaseInfo();
        accountBaseInfo.setId(-1);
        accountBaseInfo.setEmail("914577981@qq.com");

        accountContext.setAccountBaseInfo(accountBaseInfo);
        return accountContext;
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
    public boolean doAccountClaim(AccountContext accountContext) {
        String listUrl = "https://api.unich.com/airdrop/user/v1/mining/start";

        NetworkProxy proxy = accountContext.getProxy();
        String token = accountContext.getParam("token");

        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("Authorization", "Bearer " + token);

        String printPrefix = String.format("id[%s]-账户[%s]-token[%s]-proxy[%s]",
                accountContext.getAccountBaseInfo().getId(), accountContext.getName(),
                token.substring(0, Math.min(16, token.length())), proxy.getHost() + ":" + proxy.getPort());


        syncRequest(proxy, listUrl, "post", headers, null, new JSONObject(),
                () -> String.format("%s 开始mining", printPrefix))
                // Step 1.1 处理响应结果
                .thenAcceptAsync(responseStr -> {
                    if (responseStr != null) {
                        JSONObject result = JSONObject.parseObject(responseStr);
                        if (result.getString("code").equalsIgnoreCase("ok")) {
                            log.info("{} mining成功", printPrefix);

                            Long startAt = result.getJSONObject("data").getLong("submittedAt");
                            accountContext.setParam("submittedAt", String.valueOf(startAt));
                        } else {
                            log.error("{} mining失败", printPrefix);
                        }
                    } else {
                        log.error("{} mining请求失败", printPrefix);
                    }
                }, getExecutorService()).exceptionally(throwable -> {
                    log.error("{} mining请求失败", printPrefix, throwable);
                    return null;
                });

        return false;
    }

    @Override
    public CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> verifierAccountEmail(AccountContext accountContext, Message message) {
        return null;
    }

    /**
     * 开始领取社交任务
     *
     * @return print str
     */
    private String startClaimSocialReward() {
        Semaphore semaphore = new Semaphore(getBaseDepinBotConfig().getConcurrentCount());

        getAccounts().stream()
                .filter(accountContext -> !BooleanUtil.toBoolean(accountContext.getParam("social_completed")))
                .forEach(accountContext -> {
                    try {
                        semaphore.acquire();

                        NetworkProxy proxy = accountContext.getProxy();
                        String token = accountContext.getParam("token");

                        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
                        headers.put("Authorization", "Bearer " + token);

                        String printPrefix = String.format("账户[%s]-token[%s]-proxy[%s]", accountContext.getName(),
                                token.substring(0, Math.min(16, token.length())), proxy.getHost() + ":" + proxy.getPort());

                        // Step 1 获取用户的社交任务列表
                        CompletableFuture<List<String>> taskListFuture = null;
                        String unclaimedTaskIdsStr = accountContext.getParam("unclaimed_task_ids");

                        if (unclaimedTaskIdsStr != null) {
                            taskListFuture = CompletableFuture.completedFuture(JSONObject.parseArray(unclaimedTaskIdsStr, String.class));
                        } else {
                            taskListFuture = queryAccountUnClaimedTaskIds(proxy, headers, printPrefix);
                        }


                        // Step 2 开始领取任务
                        taskListFuture.
                                thenApply(taskList -> claimAccountTasks(accountContext, taskList, headers))
                                // Step 3 处理结果,未领取的标记一下
                                .thenAcceptAsync(errorIds -> {
                                    if (errorIds == null || !errorIds.isEmpty()) {
                                        accountContext.setParam("unclaimed_task_ids", JSONObject.toJSONString(errorIds));
                                    } else {
                                        accountContext.setParam("social_completed", "true");
                                    }
                                }, getExecutorService())
                                .exceptionallyAsync(throwable -> {
                                    log.error("{} 领取社交奖励发生异常", printPrefix, throwable);
                                    accountContext.setParam("social_completed", "false");
                                    return null;
                                })
                                .whenCompleteAsync((unused, throwable) -> semaphore.release());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });

        return "已开始领取账户社交奖励任务, 共[{}]" + getAccounts().size() + "个账户";
    }

    private CompletableFuture<List<String>> queryAccountUnClaimedTaskIds(NetworkProxy proxy, Map<String, String> headers, String printPrefix) {
        String listUrl = "https://api.unich.com/airdrop/user/v1/social/list-by-user";


        return syncRequest(proxy, listUrl, "get", headers, null, null,
                () -> String.format("%s 开始领取社交奖励", printPrefix))
                // Step 1.1 处理响应结果
                .thenApplyAsync(responseStr -> {
                    if (responseStr != null) {
                        JSONObject result = JSONObject.parseObject(responseStr);

                        // Step 1.2 过滤出未领取的任务
                        List<String> ids = new ArrayList<>();
                        JSONArray taskList = result.getJSONObject("data").getJSONArray("items");
                        for (int i = 0; i < taskList.size(); i++) {
                            JSONObject item = taskList.getJSONObject(i);
                            if (!item.getBoolean("claimed")) {
                                ids.add(item.getString("id"));
                            }
                        }

                        log.info("{} }获取任务列表成功", printPrefix);
                        return ids;
                    } else {
                        throw new RuntimeException(String.format("%s 任务列表失败", printPrefix));
                    }
                }, getExecutorService());
    }


    /**
     * 领取账户taskId的任务
     *
     * @param accountContext  accountContext
     * @param unclaimedTaskId unclaimedTaskId
     * @param headers         headers
     * @return List<String> 未完成的任务
     */
    private List<String> claimAccountTasks(AccountContext accountContext, List<String> unclaimedTaskId, Map<String, String> headers) {
        NetworkProxy proxy = accountContext.getProxy();
        String proxyAddress = proxy.getHost() + ":" + proxy.getPort();
        String token = accountContext.getParam("token");

        log.info("账户[{}]共[{}]个未领取的任务", accountContext.getName(), unclaimedTaskId.size());
        if (unclaimedTaskId.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 2.2 开始领取任务
        String claimUrl = "https://api.unich.com/airdrop/user/v1/social/claim/";

        List<CompletableFuture<String>> subTaskFutures = unclaimedTaskId.stream().map(taskId -> {
            JSONObject body = new JSONObject();
            body.put("evidence", taskId);

            return syncRequest(proxy, claimUrl + taskId, "post", headers, null, body,
                    () -> String.format("账户[%s]-token[%s]-proxy[%s]-taskId[%s]开始领取", accountContext.getName(), token, proxyAddress, taskId))
                    .thenApplyAsync(responseStr -> {

                        if (responseStr != null) {
                            log.info("账户[{}]-token[{}]-proxy[{}]-taskId-[{}]领取成功, {}",
                                    accountContext.getName(), token, proxyAddress, taskId, responseStr);
                            return responseStr;
                        } else {
                            log.error("获取账户[{}]-token[{}]-taskId[{}]任务领取失败",
                                    accountContext.getName(), token, taskId);
                        }
                        return taskId;
                    });
        }).toList();

        // Step 2.3 等待所有任务完成
        CompletableFuture.allOf(subTaskFutures.toArray(new CompletableFuture[0])).join();

        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < subTaskFutures.size(); i++) {
            CompletableFuture<String> future = subTaskFutures.get(i);
            try {
                future.get();
                result.add(unclaimedTaskId.get(i));
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取账户[{}]-token[{}]-taskId[{}]任务领取发生系统错误, {}",
                        accountContext.getName(), token, result.add(unclaimedTaskId.get(i)), e.getMessage());
                result.add(unclaimedTaskId.get(i));
            }
        }

        return result;
    }


    public static void main(String[] args) throws DepinBotStartException {
        UnichClaimBot unichClaimBot = new UnichClaimBot(UnichConfig.loadYamlConfig(List.of("depin", "app", "unich"), "unich.yaml"));
        unichClaimBot.init();

        MenuCMDLineAutoBot<BaseDepinBotConfig> bot = new MenuCMDLineAutoBot<>(unichClaimBot,
                List.of()
                );

    }
}
