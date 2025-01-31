package cn.com.helei.application.openloop;

import cn.com.helei.bot.core.bot.DefaultMenuCMDLineDepinBot;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;

import cn.com.helei.bot.core.exception.DepinBotStartException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@Slf4j
public class OpenLoopDepinBot extends DefaultMenuCMDLineDepinBot<OpenLoopConfig> {

    private final OpenLoopApi openLoopApi;

    public OpenLoopDepinBot(String configClassPath) {
        super(OpenLoopConfig.loadYamlConfig(configClassPath));

        openLoopApi = new OpenLoopApi(this);
    }


    @Override
    protected void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu) {
        defaultMenuTypes.add(DefaultMenuType.REGISTER);
        defaultMenuTypes.add(DefaultMenuType.LOGIN);
        defaultMenuTypes.add(DefaultMenuType.START_ACCOUNT_CLAIM);
    }

    @Override
    public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return openLoopApi.registerUser(
                accountContext,
                inviteCode
        );
    }

    @Override
    public CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return openLoopApi.loginUser(
                accountContext
        );
    }

    @Override
    public boolean doAccountClaim(AccountContext accountContext) {
        try {
            return openLoopApi.shareBandwidth(accountContext).get();
        } catch (InterruptedException | ExecutionException e) {
           log.error("账户[{}]分享带宽发生异常{}", accountContext.getName(), e.getMessage());
           return false;
        }
    }


    public static void main(String[] args) throws DepinBotStartException {
        OpenLoopDepinBot openLoopDepinBot = new OpenLoopDepinBot("openloop.yaml");
        openLoopDepinBot.init();
        openLoopDepinBot.start();
    }
}
