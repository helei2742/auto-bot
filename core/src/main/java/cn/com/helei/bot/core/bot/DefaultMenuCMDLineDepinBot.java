package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.BaseDepinBotConfig;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public abstract class DefaultMenuCMDLineDepinBot<C extends BaseDepinBotConfig> extends CommandLineDepinBot {


    private static final String INVITE_CODE_KEY = "inviteCode";

    /**
     * 刷新节点
     */
    public static final CommandMenuNode REFRESH_NODE = new CommandMenuNode(true, "刷新", "当前数据已刷新", null);

    /**
     * 是否开始过链接所有账号
     */
    private final AtomicBoolean isStartAccountConnected = new AtomicBoolean(false);


    private final List<DefaultMenuType> defaultMenuTypes = new ArrayList<>(List.of(
            DefaultMenuType.ACCOUNT_LIST,
            DefaultMenuType.PROXY_LIST,
            DefaultMenuType.BROWSER_ENV_LIST
    ));

    @Getter
    private final C botConfig;

    public DefaultMenuCMDLineDepinBot(C botConfig) {
        super(botConfig);

        this.botConfig = botConfig;
    }


    @Override
    protected CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    protected void buildMenuNode(CommandMenuNode mainManu) {
        addCustomMenuNode(defaultMenuTypes, mainManu);

        for (DefaultMenuType menuType : defaultMenuTypes) {
            mainManu.addSubMenu(switch (menuType) {
                case REGISTER -> buildRegisterMenuNode();
                case LOGIN -> buildQueryTokenMenuNode();
                case ACCOUNT_LIST -> buildAccountListMenuNode();
                case PROXY_LIST -> buildProxyListMenuNode();
                case BROWSER_ENV_LIST -> buildBrowserListMenuNode();
                case START_ACCOUNT_CLAIM -> buildStartAccountConnectMenuNode();
            });
        }
    }


    /**
     * 构建注册菜单节点
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildRegisterMenuNode() {
        CommandMenuNode registerMenu = new CommandMenuNode("注册",
                "请确认邀请码后运行", this::printCurrentInvite);

        CommandMenuNode interInvite = new CommandMenuNode(
                "填入邀请码",
                "请输入邀请码：",
                this::printCurrentInvite
        );
        interInvite.setResolveInput(input -> {
            log.info("邀请码修改[{}]->[{}]", botConfig.getConfig(INVITE_CODE_KEY), input);
            botConfig.setConfig(INVITE_CODE_KEY, input);
        });


        return registerMenu.addSubMenu(interInvite)
                .addSubMenu(new CommandMenuNode(
                        "开始注册",
                        "开始注册所有账号...",
                        this::registerAllAccount
                ));
    }

    /**
     * 获取token
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildQueryTokenMenuNode() {
        return new CommandMenuNode("获取token", "开始获取所有账号token...", this::loadAllAccountToken);
    }

    /**
     * 构建查看代理列表的菜单节点
     *
     * @return 查看代理列表菜单节点
     */
    private CommandMenuNode buildProxyListMenuNode() {
        return new CommandMenuNode(
                "查看代理列表",
                "当前代理列表文件:" + getProxyPool().getConfigClassPath(),
                getProxyPool()::printPool
        ).addSubMenu(REFRESH_NODE);
    }

    /**
     * 构建查看浏览器环境列表的菜单节点
     *
     * @return 查看浏览器环境列表菜单节点
     */
    private CommandMenuNode buildBrowserListMenuNode() {
        return new CommandMenuNode(
                "查看浏览器环境列表",
                "当前代理列表文件:" + getBrowserEnvPool().getConfigClassPath(),
                getBrowserEnvPool()::printPool
        ).addSubMenu(REFRESH_NODE);
    }

    /**
     * 账户列表菜单节点
     *
     * @return 账户列表节点
     */
    private CommandMenuNode buildAccountListMenuNode() {
        CommandMenuNode accountListMenuNode = new CommandMenuNode(
                "查看账号",
                "当前账户详情列表:",
                this::printAccountList
        );

        return accountListMenuNode
                .addSubMenu(buildAccountRewardMenuNode())
                .addSubMenu(buildAccountConnectStatusMenuNode())
                .addSubMenu(REFRESH_NODE);
    }

    /**
     * 查看账户收益菜单节点
     *
     * @return 账户收益节点
     */
    private CommandMenuNode buildAccountRewardMenuNode() {
        return new CommandMenuNode(
                "查看账号收益",
                "账号收益详情列表:",
                this::printAccountReward
        ).addSubMenu(REFRESH_NODE);
    }

    /**
     * 查看账户连接情况菜单节点
     *
     * @return 账户收益节点
     */
    private CommandMenuNode buildAccountConnectStatusMenuNode() {
        return new CommandMenuNode(
                "查看账号连接情况",
                "账号连接情况列表:",
                this::printAccountConnectStatusList
        ).addSubMenu(REFRESH_NODE);
    }


    /**
     * 开始账户连接菜单节点
     *
     * @return 连接账户菜单节点
     */
    private CommandMenuNode buildStartAccountConnectMenuNode() {
        CommandMenuNode menuNode = new CommandMenuNode(
                "启动账号",
                "启动账号界面，",
                this::startAccountsClaim
        );

        CommandMenuNode refresh = new CommandMenuNode(true, "刷新", "当前账户列表",
                this::printAccountList);

        menuNode.addSubMenu(refresh);
        return menuNode;
    }

    /**
     * 注册所有账号
     *
     * @return String
     */
    private String registerAllAccount() {
        CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<Boolean>> futures = getAccounts().stream()
                    .map(account -> {
                        // 账户注册过，
                        if (BooleanUtil.isTrue(account.getClientAccount().getSignUp())) {
                            log.warn("账户[{}]-email[{}]注册过", account.getName(), account.getClientAccount().getEmail());
                            return CompletableFuture.completedFuture(false);
                        } else {
                            return registerAccount(account, botConfig.getConfig(INVITE_CODE_KEY));
                        }
                    }).toList();

            int successCount = 0;
            for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<Boolean> future = futures.get(i);
                AccountContext accountContext = getAccounts().get(i);
                try {
                    if (future.get()) {
                        //注册成功
                        successCount++;
                        accountContext.getClientAccount().setSignUp(true);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("注册账号[{}]发生错误, {}", accountContext.getName(), e.getMessage());
                }
            }

            return String.format("所有账号注册完毕，[%d/%d]", successCount, getAccounts().size());
        }, getExecutorService());

        return "已开始账户注册";
    }

    /**
     * 获取账号的token
     *
     * @return String
     */
    public String loadAllAccountToken() {
        List<CompletableFuture<String>> futures = getAccounts().stream()
                .map(this::requestTokenOfAccount).toList();

        int successCount = 0;
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<String> future = futures.get(i);
            AccountContext accountContext = getAccounts().get(i);
            try {
                String token = future.get();
                if (StrUtil.isNotBlank(token)) {
                    successCount++;
                    accountContext.getParams().put("token", token);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("账号[{}]获取token发生错误, {}", accountContext.getName(), e.getMessage());
            }
        }

        return String.format("所有账号获取token完毕，[%d/%d]", successCount, getAccounts().size());
    }


    /**
     * 开始所有账户Claim
     *
     * @return String 打印的消息
     */
    private String startAccountsClaim() {
        if (isStartAccountConnected.compareAndSet(false, true)) {

            doAccountsClaim();

            return "已开始账号自动收获";
        }

        return "账号自动收获中";
    }

    /**
     * 开始账户claim
     */
    protected void doAccountsClaim() {
        getAccounts().forEach(account -> {
            account.getConnectStatusInfo().setStartDateTime(LocalDateTime.now());

            addTimer(
                    () -> doAccountClaim(account),
                    getBotConfig().getAutoClaimIntervalSeconds(),
                    TimeUnit.SECONDS,
                    account
            );
        });
    }

    /**
     * 打印当前的邀请码
     *
     * @return 邀请码
     */
    private String printCurrentInvite() {
        String inviteCode = botConfig.getConfigMap().get("inviteCode");
        return "(当前邀请码为:" + inviteCode + ")";
    }

    /**
     * 添加自定义菜单节点, defaultMenuTypes中可额外添加菜单类型
     *
     * @param defaultMenuTypes defaultMenuTypes
     * @param mainMenu         mainMenu
     */
    protected abstract void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu);

    /**
     * 注册账户
     *
     * @param accountContext accountContext
     * @param inviteCode     inviteCode
     * @return CompletableFuture<Boolean> 是否注册成功
     */
    protected abstract CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode);

    /**
     * 请求获取账户token
     *
     * @param accountContext accountContext
     * @return CompletableFuture<String> token
     */
    protected abstract CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext);


    /**
     * 开始账户自动收获,会自动循环。返回false则跳出自动循环
     *
     * @return CompletableFuture<Void>
     */
    protected abstract boolean doAccountClaim(AccountContext accountContext);

}
