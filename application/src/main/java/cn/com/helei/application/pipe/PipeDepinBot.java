package cn.com.helei.application.pipe;

import cn.com.helei.bot.core.bot.DefaultMenuCMDLineDepinBot;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PipeDepinBot extends DefaultMenuCMDLineDepinBot<PipeConfig> {

    public PipeDepinBot(PipeConfig botConfig) {
        super(botConfig);
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

    private CompletableFuture<String> queryToken(AccountContext accountContext) {
        return getBaseUrl(accountContext)
                .thenApplyAsync(baseUrl->{
                   return null;
                });
    }

    /**
     * 查询base url
     *
     * @param accountContext accountContext
     * @return base url
     */
    private CompletableFuture<String> getBaseUrl(AccountContext accountContext) {
        Map<String, String> headers = accountContext.getBrowserEnv().getHeaders();

        return syncRequest(
                accountContext.getProxy(),
                getBotConfig().getGetBaseUrlQueryUrl(),
                "get",
                headers,
                null,
                null
        );
    }
}
