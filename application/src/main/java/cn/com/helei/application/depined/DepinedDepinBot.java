package cn.com.helei.application.depined;

import cn.com.helei.bot.core.bot.DefaultMenuCMDLineDepinBot;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;


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
    public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return null;
    }

    @Override
    public CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return null;
    }

    @Override
    public boolean doAccountClaim(AccountContext accountContext) {
        return false;
    }


    private String registerAccountAction() {
        return null;
    }
}
