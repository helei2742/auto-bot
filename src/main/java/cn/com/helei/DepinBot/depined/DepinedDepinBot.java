package cn.com.helei.DepinBot.depined;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.BaseDepinWSClient;
import cn.com.helei.DepinBot.core.bot.CommandLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;
import com.alibaba.fastjson.JSONObject;

public class DepinedDepinBot extends CommandLineDepinBot<JSONObject, JSONObject> {


    public DepinedDepinBot(BaseDepinBotConfig baseDepinBotConfig) {
        super(baseDepinBotConfig);
    }

    @Override
    protected void buildMenuNode(CommandMenuNode mainManu) {
        mainManu.addSubMenu(new CommandMenuNode("账户注册", "开始注册账户", this::registerAccountAction));
    }


    private String registerAccountAction() {
        return null;
    }


    @Override
    public BaseDepinWSClient<JSONObject, JSONObject> buildAccountWSClient(AccountContext accountContext) {
        return null;
    }

    @Override
    public void whenAccountConnected(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, Boolean success) {

    }

    @Override
    public void whenAccountReceiveResponse(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, String id, JSONObject response) {

    }

    @Override
    public void whenAccountReceiveMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient, JSONObject message) {

    }

    @Override
    public JSONObject getHeartbeatMessage(BaseDepinWSClient<JSONObject, JSONObject> depinWSClient) {
        return null;
    }

}
