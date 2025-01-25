package cn.com.helei.DepinBot.app.depined;

import cn.com.helei.DepinBot.core.bot.DefaultMenuCMDLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.commandMenu.DefaultMenuType;
import cn.com.helei.DepinBot.core.dto.account.AccountContext;


import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DepinedDepinBot extends DefaultMenuCMDLineDepinBot<DepinedConfig> {


    public DepinedDepinBot(DepinedConfig baseDepinBotConfig) {
        super(baseDepinBotConfig);
    }

    @Override
    protected void buildMenuNode(CommandMenuNode mainManu) {
        mainManu.addSubMenu(new CommandMenuNode("账户注册", "开始注册账户", this::registerAccountAction));
    }

    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {

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


    private String registerAccountAction() {
        return null;
    }
}
