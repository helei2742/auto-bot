package cn.com.helei.application.unich;

import cn.com.helei.bot.core.bot.RestTaskAutoBot;
import cn.com.helei.bot.core.bot.view.MenuCMDLineAutoBot;
import cn.com.helei.bot.core.config.BaseAutoBotConfig;
import cn.com.helei.bot.core.dto.account.AccountBaseInfo;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.exception.DepinBotStartException;
import cn.com.helei.bot.core.supporter.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.supporter.commandMenu.MenuNodeMethod;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.mail.Message;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class UnichClaimBot extends RestTaskAutoBot {

    public static final String TOKEN_EXPIRE_TIME_KEY = "token_expire_time";

    private final UnichApi unichApi;

    public UnichClaimBot(UnichConfig botConfig) {
        super(botConfig);
        this.unichApi = new UnichApi(this);
    }

    @NotNull
    private AccountContext loadMainAccount() {
        AccountContext accountContext = new AccountContext();
        accountContext.setParam("token", "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI2NzkyMzA5M2ZkNWFjM2E2YWQ3M2VjOTMiLCJzaWduZWRBdCI6MTczNzczNDM0MTAzMiwiaWQiOiI0U1FLVE1ZTlUyQ1VDSDBTIiwidHlwZSI6ImFjY2Vzc1Rva2VuIn0.8Buae8mnHJ0Ur3eUPdaHXCwCjNbOc-a90xvJKSSZ-DI");
        accountContext.setProxy(getStaticProxyPool().getLessUsedItem(1).getFirst());
        accountContext.setBrowserEnv(getBrowserEnvPool().getLessUsedItem(1).getFirst());
        AccountBaseInfo accountBaseInfo = new AccountBaseInfo();
        accountBaseInfo.setId(-1);
        accountBaseInfo.setEmail("914577981@qq.com");

        accountContext.setAccountBaseInfo(accountBaseInfo);
        return accountContext;
    }


    @Override
    public CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode) {
        return null;
    }

    @Override
    public CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext) {
        return unichApi.loginAndTakeToken(accountContext);
    }

    @Override
    public boolean doAccountClaim(AccountContext accountContext) {
        return unichApi.startMining(accountContext);
    }

    @Override
    public CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> verifierAccountEmail(AccountContext accountContext, Message message) {
        return null;
    }

    /**
     * 开始领取社交任务
     *
     * @return print str
     */
    @MenuNodeMethod(title = "领取社交奖励", description = "开始领取社交奖励")
    private String startClaimSocialReward() {
        return unichApi.startClaimSocialReward();
    }

    public static void main(String[] args) throws DepinBotStartException {
        UnichClaimBot unichClaimBot = new UnichClaimBot(UnichConfig.loadYamlConfig(List.of("depin", "app", "unich"), "unich.yaml"));
        MenuCMDLineAutoBot<BaseAutoBotConfig> bot = new MenuCMDLineAutoBot<>(unichClaimBot,
                List.of(DefaultMenuType.START_ACCOUNT_CLAIM, DefaultMenuType.LOGIN)
        );

        bot.start();
    }
}
