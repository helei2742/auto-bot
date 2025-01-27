package cn.com.helei.DepinBot.app.layeredge;

import cn.com.helei.DepinBot.core.bot.DefaultMenuCMDLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.commandMenu.DefaultMenuType;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import cn.com.helei.DepinBot.core.exception.DepinBotInitException;
import cn.com.helei.DepinBot.core.pool.network.NetworkProxy;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class LayeredgeDepinBot extends DefaultMenuCMDLineDepinBot<LayeredgeConfig> {

    public LayeredgeDepinBot(LayeredgeConfig botConfig) {
        super(botConfig);
    }

    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {

        defaultMenuTypes.add(DefaultMenuType.START_ACCOUNT_CLAIM);
        mainMenu.addSubMenu(
                new CommandMenuNode("每日签到", "正在开始每日签到", this::dailySignIn)
        );
    }

    @Override
    protected void doInit() throws DepinBotInitException {
        super.doInit();

        // 将tokens加载到对应的accountContext
        List<AccountContext> accounts = getAccounts();

        List<String> publicKeys = getBotConfig().getPublicKeys();
        if (publicKeys == null) return;

        for (int i = 0; i < publicKeys.size(); i++) {
            if (i < accounts.size()) {
                accounts.get(i).setParam("publicKey", publicKeys.get(i));
            }
        }
    }

    @Override
    protected CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return null;
    }

    @Override
    protected CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return null;
    }

    @Override
    protected boolean doAccountClaim(AccountContext accountContext) {
        String url = "https://referral.layeredge.io/api/light-node/node-status/";
        NetworkProxy proxy = accountContext.getProxy();
        Map<String, String> headers = getHeaders(accountContext);

        String address = accountContext.getParam("publicKey");

        String pintStr = String.format("id[%s]-账户[%s]-address[%s]-proxy[%s/%d]",
                accountContext.getClientAccount().getId(), accountContext.getName(), address, proxy.getAddress(), proxy.getPort()
        );

        syncRequest(
                proxy,
                url + address,
                "get",
                headers,
                null,
                null,
                () -> {
                    accountContext.getConnectStatusInfo().getHeartBeat().incrementAndGet();
                    return pintStr + " ping服务器";
                })
                .thenAcceptAsync(responseStr -> {
                    log.info("{} ping服务器成功, {}", pintStr, responseStr);

                    accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());

                }, getExecutorService())
                .exceptionally(throwable -> {
                    log.error("{} ping服务器成失败, {}", pintStr, throwable.getMessage());
                    accountContext.getConnectStatusInfo().getErrorHeartBeat().incrementAndGet();
                    return null;
                });

        return true;
    }

    @Override
    protected CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
        String url = "https://referral.layeredge.io/api/referral/wallet-details/";
        NetworkProxy proxy = accountContext.getProxy();
        Map<String, String> headers = getHeaders(accountContext);

        String address = accountContext.getParam("publicKey");

        String pintStr = String.format("id[%s]-账户[%s]-address[%s]-proxy[%s/%d]",
                accountContext.getClientAccount().getId(), accountContext.getName(), address, proxy.getAddress(), proxy.getPort()
        );

        return syncRequest(
                proxy,
                url + address,
                "get",
                headers,
                null,
                null,
                () -> pintStr + " 查询奖励"
        )
                .thenApplyAsync(responseStr -> {
                    log.info("{} 查询奖励成功, {}", pintStr, responseStr);
                    return true;
                }, getExecutorService())
                .exceptionally(throwable -> {
                    log.error("{} 查询奖励失败,", pintStr, throwable);
                    return false;
                });
    }

    private String dailySignIn() {
        getAccounts().forEach(accountContext -> {
            if (accountContext.getClientAccount().getId() != 0) return;
            String url = "https://dashboard.layeredge.io/api/claim-points";

            NetworkProxy proxy = accountContext.getProxy();
            Map<String, String> headers = getHeaders(accountContext);

            JSONObject body = new JSONObject();
            String address = accountContext.getParam("publicKey");
            body.put("walletAddress", address);

            String pintStr = String.format("id[%s]-账户[%s]-address[%s]-proxy[%s/%d]",
                    accountContext.getClientAccount().getId(), accountContext.getName(), address, proxy.getAddress(), proxy.getPort()
            );

            syncRequest(
                    proxy,
                    url,
                    "post",
                    headers,
                    null,
                    body,
                    () -> pintStr
            )
                    .thenAcceptAsync(responseStr -> {
                        log.info("{} 每日奖励领取成功, {}", pintStr, responseStr);
                    }, getExecutorService())
                    .exceptionally(throwable -> {
                        log.error("{} 每日奖励领取失败,", pintStr, throwable);
                        return null;
                    });
        });

        return "已开始领取每日奖励";
    }

    private static Map<String, String> getHeaders(AccountContext accountContext) {
        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
        headers.put("origin", "https://dashboard.layeredge.io");
        headers.put("referer", "https://dashboard.layeredge.io/");
        return headers;
    }
}
