package cn.com.helei.application.openloop;

import cn.com.helei.bot.core.bot.RestTaskAutoBot;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.dto.account.AccountContext;

import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.supporter.commandMenu.DefaultMenuType;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@Slf4j
public class OpenLoopAutoBot extends RestTaskAutoBot {

    private final OpenLoopApi openLoopApi;

    public OpenLoopAutoBot(String configClassPath) {
        super(OpenLoopConfig.loadYamlConfig(configClassPath));

        openLoopApi = new OpenLoopApi(this);
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

    @Override
    public CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> verifierAccountEmail(AccountContext accountContext, Message message) {
        return null;
    }


    public static void main(String[] args) throws DepinBotStartException {
        OpenLoopAutoBot openLoopDepinBot = new OpenLoopAutoBot("openloop.yaml");
        MenuCMDLineAutoBot<OpenLoopConfig> menuCMDLineAutoBot = new MenuCMDLineAutoBot<>(openLoopDepinBot,
                List.of(DefaultMenuType.REGISTER, DefaultMenuType.LOGIN, DefaultMenuType.START_ACCOUNT_CLAIM));

        menuCMDLineAutoBot.start();

    }
}
