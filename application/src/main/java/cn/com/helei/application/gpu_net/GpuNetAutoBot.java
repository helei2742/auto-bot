package cn.com.helei.application.gpu_net;

import cn.com.helei.bot.core.bot.DefaultMenuCMDLineDepinBot;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.config.SystemConfig;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.DepinBotInitException;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.util.YamlConfigLoadUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

@Slf4j
public class GpuNetAutoBot extends DefaultMenuCMDLineDepinBot<GpuNetConfig> {

    private static final String SOL_ADDRESS_KEY = "sol_address";

    private static final String WHITE_LIST_SUCCESS_KEY = "white_list_success";

    private final Semaphore semaphore = new Semaphore(10);

    public GpuNetAutoBot(GpuNetConfig botConfig) {
        super(botConfig);
    }

    @Override
    protected void doInit() throws DepinBotInitException {
        super.doInit();

        List<String> solAddress = getBotConfig().getSolAddress();

        List<AccountContext> accounts = getAccounts();

        for (int i = 0; i < solAddress.size(); i++) {
            if (i < accounts.size()) {
                accounts.get(i).setParam(SOL_ADDRESS_KEY, solAddress.get(i));
            }
        }
    }

    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {
        mainMenu.addSubMenu(new CommandMenuNode("GPU.NET白名单",
                "https://sol.gpu.net 白名单自动填写", this::gpuNetWhiteListEnter));
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
        return false;
    }

    private String gpuNetWhiteListEnter() {
        List<AccountContext> accounts = getAccounts();
        List<CompletableFuture<Boolean>> futures = accounts.stream().map(accountContext -> {
            if ("true".equals(accountContext.getParam(WHITE_LIST_SUCCESS_KEY)))
                return CompletableFuture.completedFuture(true);

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String url = "https://form-collection.subscriptions-653.workers.dev/register";

            String solAddress = accountContext.getParam(SOL_ADDRESS_KEY);
            String twitterName = accountContext.getTwitter().getUsername();

            JSONObject body = new JSONObject();
            body.put("email", accountContext.getClientAccount().getEmail());
            body.put("solanaAddress", solAddress);
            body.put("twitterHandle", twitterName);

            Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();
            headers.put("Origin", "https://sol.gpu.net");
            headers.put("referer", "https://sol.gpu.net/");

            String printStr = String.format("账户[%s]-sol地址[%s]-推特[%s]-proxy[%s]",
                    accountContext.getName(), solAddress, twitterName, accountContext.getProxy().getAddressStr());

            return syncRequest(
                    accountContext.getProxy(),
                    url,
                    "post",
                    headers,
                    null,
                    body,
                    () -> printStr + "开始发送白名单请求信息"
            ).thenApplyAsync(responseStr -> {
                        JSONObject response = JSONObject.parseObject(responseStr);
                        if (response.getBooleanValue("success")) {
                            log.info("{} 白名单提交成功, {}", printStr, responseStr);
                            accountContext.setParam(WHITE_LIST_SUCCESS_KEY, "true");
                            return true;
                        } else {
                            log.error("{} 白名单提交失败，{}", printStr, responseStr);
                            accountContext.setParam(WHITE_LIST_SUCCESS_KEY, "false");
                            return false;
                        }
                    }, getExecutorService())
                    .exceptionally(throwable -> {
                        log.error("{} 白名单提交发生异常, {}", printStr, throwable.getMessage());
                        accountContext.setParam(WHITE_LIST_SUCCESS_KEY, "false");
                        return false;
                    }).whenCompleteAsync((success, throwable) -> {
                        semaphore.release();
                    });
        }).toList();

        List<Integer> errorAccount = new ArrayList<>();
        int successCount = 0;
        for (int i = 0; i < futures.size(); i++) {

            try {
                if (futures.get(i).get()) {
                    successCount++;
                    errorAccount.add(accounts.get(i).getClientAccount().getId());
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return "已开始提交所有白名单申请, 成功:" + successCount + " 共: " + accounts.size() + "\n错误列表：" + errorAccount;
    }

    public static void main(String[] args) throws DepinBotStartException {
        GpuNetConfig netConfig = YamlConfigLoadUtil.load(SystemConfig.CONFIG_DIR_APP_PATH,
                "gpunet.yaml", List.of("bot", "app", "gpunet"), GpuNetConfig.class);

        GpuNetAutoBot bot = new GpuNetAutoBot(netConfig);

        bot.init();;

        bot.start();
    }
}
