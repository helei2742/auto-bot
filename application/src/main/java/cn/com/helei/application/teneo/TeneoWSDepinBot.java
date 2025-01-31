package cn.com.helei.application.teneo;

import cn.com.helei.bot.core.BaseDepinWSClient;
import cn.com.helei.bot.core.SimpleDepinWSClient;
import cn.com.helei.bot.core.bot.WSMenuCMDLineDepinBot;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.netty.constants.WebsocketClientStatus;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.extern.slf4j.Slf4j;


import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class TeneoWSDepinBot extends WSMenuCMDLineDepinBot<TeneoDepinConfig, JSONObject, JSONObject> {

    private final TeneoApi teneoApi;

    public TeneoWSDepinBot(TeneoDepinConfig config) {
        super(config);

        this.teneoApi = new TeneoApi(this);
    }

    @Override
    public BaseDepinWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {

        accountContext.setConnectUrl("wss://secure.ws.teneo.pro/websocket?accessToken="
                + accountContext.getParam("token") + "&version=v0.2");

        SimpleDepinWSClient simpleDepinWSClient = new SimpleDepinWSClient(this, accountContext);

        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        Map<String, String> originHeaders = accountContext.getBrowserEnv().getHeaders();
        originHeaders.forEach(headers::add);

        headers.add("Host", "secure.ws.teneo.pro");
        headers.add("Origin", "chrome-extension://emcclcoaglgcpoognfiggmhnhgabppkm");

        simpleDepinWSClient.setHeaders(headers);

        return simpleDepinWSClient;
    }

    @Override
    public void whenAccountClientStatusChange(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, WebsocketClientStatus clientStatus) {

        final AccountContext accountContext = depinWSClient.getAccountContext();

        switch (clientStatus) {
            case STARTING -> {
                log.info("账户[{}]-proxy[{}] 开始连接ws服务器[{}]",
                        accountContext.getName(), accountContext.getProxy().getAddressStr(), accountContext.getConnectUrl());

            }
            case RUNNING -> {
                log.info("账户[{}]-proxy[{} ]已连接到ws服务器",
                        accountContext.getName(), accountContext.getProxy().getAddressStr());

                depinWSClient.sendMessage(getHeartbeatMessage(depinWSClient));
            }
            case STOP -> {
                log.warn("账户[{}]-proxy[{}]已断开连接",
                        accountContext.getName(), accountContext.getProxy().getAddressStr());
                accountContext.getConnectStatusInfo().getRestart().incrementAndGet();
            }
            case SHUTDOWN ->{
                log.warn("账户[{}]-proxy[{}] 工作已停止",
                        accountContext.getName(), accountContext.getProxy().getAddressStr());
            }
        }
    }

    @Override
    public void whenAccountReceiveResponse(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, Object id, JSONObject response) {

    }

    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, JSONObject message) {
        String type = message.getString("message");
        AccountContext accountContext = depinWSClient.getAccountContext();

        if ("Connected successfully".equals(type)) {
            Double pointsToday = message.getDouble("pointsToday");
            Double pointsTotal = message.getDouble("pointsTotal");

            accountContext.getRewordInfo().setTodayPoints(pointsToday);
            accountContext.getRewordInfo().setTotalPoints(pointsTotal);

            log.info("账户[{}]-proxy[{}] 连接成功. 今日积分: {}, 总积分: {}",
                    accountContext.getName(), accountContext.getProxy().getAddressStr(),
                    pointsToday, pointsTotal);

        } else if ("Pulse from server".equals(type)) {
            Double pointsToday = message.getDouble("pointsToday");
            Double pointsTotal = message.getDouble("pointsTotal");

            accountContext.getRewordInfo().setTodayPoints(pointsToday);
            accountContext.getRewordInfo().setTotalPoints(pointsTotal);

            int heartbeatToday = message.getInteger("heartbeats");

            //减去提前加上的错误心跳数
            accountContext.getConnectStatusInfo().getErrorHeartBeat().decrementAndGet();
            log.info("账户[{}]-proxy[{}] 心跳发送成功. 今日心跳: {}",
                    accountContext.getName(), accountContext.getProxy().getAddressStr(),
                    heartbeatToday);
        }
    }

    @Override
    public JSONObject getHeartbeatMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient) {
        ConnectStatusInfo connectStatusInfo = depinWSClient.getAccountContext().getConnectStatusInfo();
        connectStatusInfo.getHeartBeat().incrementAndGet();

        // 错误心跳数提前加上
        connectStatusInfo.getErrorHeartBeat().incrementAndGet();

        JSONObject ping = new JSONObject();
        ping.put("type", "ping");

        log.info("{} 发送ping", depinWSClient.getAccountContext().getSimpleInfo());
        return ping;
    }

    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {
        defaultMenuTypes.add(DefaultMenuType.LOGIN);
        defaultMenuTypes.add(DefaultMenuType.REGISTER);
        defaultMenuTypes.add(DefaultMenuType.START_ACCOUNT_CLAIM);

        mainMenu.addSubMenu(new CommandMenuNode("验证邮箱", "开始验证注册邮箱", teneoApi::verifierEmail));
    }

    @Override
    public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return teneoApi.registerAccount(accountContext, inviteCode);
    }

    @Override
    public CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return teneoApi.login(accountContext);
    }

    public static void main(String[] args) throws DepinBotStartException {
        TeneoDepinConfig teneoDepinConfig = TeneoDepinConfig.loadYamlConfig("bot.app.teneo", "teneo/teneo.yaml", TeneoDepinConfig.class);

        TeneoWSDepinBot teneoWSDepinBot = new TeneoWSDepinBot(teneoDepinConfig);

        teneoWSDepinBot.init();

        teneoWSDepinBot.start();
    }
}
