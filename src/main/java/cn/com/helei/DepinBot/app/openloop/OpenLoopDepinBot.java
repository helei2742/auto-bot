//package cn.com.helei.DepinBot.openloop;
//
//import cn.com.helei.DepinBot.core.bot.DefaultMenuCMDLineDepinBot;
//import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
//import cn.com.helei.DepinBot.core.commandMenu.DefaultMenuType;
//import cn.com.helei.DepinBot.core.dto.account.AccountContext;
//import cn.com.helei.DepinBot.core.dto.account.ConnectStatusInfo;
//import com.alibaba.fastjson.JSONObject;
//import lombok.extern.slf4j.Slf4j;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
//
//@Slf4j
//public class OpenLoopDepinBot extends DefaultMenuCMDLineDepinBot<OpenLoopConfig> {
//
//    private final OpenLoopApi openLoopApi;
//
//    public OpenLoopDepinBot(String configClassPath) {
//        super(OpenLoopConfig.loadYamlConfig(configClassPath));
//
//        openLoopApi = new OpenLoopApi(this);
//    }
//
//
//    @Override
//    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {
//
//    }
//
//    @Override
//    protected CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
//        return openLoopApi.registerUser(
//                accountContext,
//                inviteCode
//        );
//    }
//
//    @Override
//    protected CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
//        return openLoopApi.loginUser(
//                accountContext
//        );
//    }
//
//
//    @Override
//    public CompletableFuture<Void> connectAllAccount() {
//        return CompletableFuture.runAsync(()-> {
//            //Step 1 设置启动时间
//            getAccounts().forEach(accountContext ->
//                    accountContext.getConnectStatusInfo().setStartDateTime(LocalDateTime.now()));
//
//            //Step 2 设置定时任务
//            addTimer(() -> {
//                List<AccountContext> accounts = getAccounts();
//
//                List<CompletableFuture<JSONObject>> futures = accounts
//                        .stream()
//                        .map(account -> {
//                            // 发起网络请求，心跳数++
//                            account.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
//                            account.getConnectStatusInfo().getHeartBeatCount().incrementAndGet();
//
//                            return openLoopApi.shareBandwidth(account);
//                        })
//                        .toList();
//
//                // 获取异步结果，处理心跳数和收益数据
//                for (int i = 0; i < futures.size(); i++) {
//                    CompletableFuture<JSONObject> future = futures.get(i);
//                    AccountContext accountContext = accounts.get(i);
//
//                    ConnectStatusInfo connectStatusInfo = accountContext.getConnectStatusInfo();
//                    try {
//                        JSONObject balances = future.get();
////                        Double totalPoint = balances.getDouble("POINT");
////
////                        accountContext.getRewordInfo().setSessionPoints(totalPoint);
////                        accountContext.getRewordInfo().setUpdateTime(LocalDateTime.now());
//                    } catch (ExecutionException | InterruptedException e) {
//                        connectStatusInfo.getErrorHeartBeatCount().getAndIncrement();
//                        connectStatusInfo.setUpdateDateTime(LocalDateTime.now());
//
//                        log.error("账号[{}]分享带宽发生异常, {}", accountContext.getName(), e.getMessage());
//                    }
//                }
//            }, 60 * 2, TimeUnit.SECONDS);
//        }, getExecutorService());
//    }
//}
