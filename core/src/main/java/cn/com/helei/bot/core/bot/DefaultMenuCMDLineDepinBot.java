package cn.com.helei.bot.core.bot;

import cn.com.helei.bot.core.config.BaseDepinBotConfig;
import cn.com.helei.bot.core.commandMenu.CommandMenuNode;
import cn.com.helei.bot.core.commandMenu.DefaultMenuType;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.supporter.AccountInfoPrinter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


@Slf4j
public abstract class DefaultMenuCMDLineDepinBot<C extends BaseDepinBotConfig> extends CommandLineDepinBot {

    /**
     * 刷新节点
     */
    public static final CommandMenuNode REFRESH_NODE = new CommandMenuNode(true, "刷新", "当前数据已刷新", null);


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
    public CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext) {
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
     * 添加自定义菜单节点, defaultMenuTypes中可额外添加菜单类型
     *
     * @param defaultMenuTypes defaultMenuTypes
     * @param mainMenu         mainMenu
     */
    protected abstract void addCustomMenuNode(List<DefaultMenuType> defaultMenuTypes, CommandMenuNode mainMenu);

    /**
     * 构建注册菜单节点
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildRegisterMenuNode() {
        CommandMenuNode registerMenu = new CommandMenuNode("注册",
                "请确认设置后运行", this::printCurrentRegisterConfig);

        CommandMenuNode interInvite = new CommandMenuNode(
                "填入邀请码",
                "请输入邀请码：",
                this::printCurrentRegisterConfig
        );
        interInvite.setResolveInput(input -> {
            log.info("邀请码修改[{}]->[{}]", botConfig.getConfig(INVITE_CODE_KEY), input);
            botConfig.setConfig(INVITE_CODE_KEY, input);
        });

        CommandMenuNode typeSelect = new CommandMenuNode(
                "选择账户类型",
                "请选择账户类型",
                this::printCurrentRegisterConfig
        );

        List<String> typeList = new ArrayList<>(getTypedAccountMap().keySet());
        for (String type : typeList) {
            CommandMenuNode typeInput = new CommandMenuNode(
                    true,
                    type + " 账户",
                    "type",
                    () -> {
                        botConfig.setConfig(REGISTER_TYPE_KEY, type);
                        return "注册[" + type + "]类型账户，共：" + getTypedAccountMap().get(type).size();
                    }
            );

            typeSelect.addSubMenu(typeInput);
        }


        return registerMenu
                .addSubMenu(interInvite)
                .addSubMenu(typeSelect)
                .addSubMenu(new CommandMenuNode(
                        "开始注册",
                        "开始注册所有账号...",
                        this::registerTypeAccount
                ));
    }

    /**
     * 获取token
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode buildQueryTokenMenuNode() {
        CommandMenuNode menuNode = new CommandMenuNode("获取token", "请选择邮箱类型", null);

        for (String type : getTypedAccountMap().keySet()) {
            CommandMenuNode typeInput = new CommandMenuNode(
                    true,
                    type + " 账户",
                    "type",
                    () -> this.loadTypedAccountToken(type)
            );
            menuNode.addSubMenu(typeInput);
        }
        return menuNode;
    }

    /**
     * 构建查看代理列表的菜单节点
     *
     * @return 查看代理列表菜单节点
     */
    private CommandMenuNode buildProxyListMenuNode() {
        return new CommandMenuNode(
                "查看代理列表",
                "当前代理列表文件:" + getStaticProxyPool().getConfigClassPath(),
                getStaticProxyPool()::printPool
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
                () -> AccountInfoPrinter.printAccountList(getTypedAccountMap())
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
                () -> AccountInfoPrinter.printAccountReward(getTypedAccountMap())
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
                () -> AccountInfoPrinter.printAccountConnectStatusList(getTypedAccountMap())
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
                "选择启动账号类型",
                null
        );

        Set<String> typeSet = getTypedAccountMap().keySet();
        for (String type : typeSet) {
            CommandMenuNode typeInput = new CommandMenuNode(true, type + " 账户", "type",
                    () -> startAccountsClaim(type, getTypedAccountMap().get(type))
            );

            menuNode.addSubMenu(typeInput);
        }
        menuNode.addSubMenu(new CommandMenuNode(true, "全部类型账户", "", () -> {
            getTypedAccountMap().forEach(this::startAccountsClaim);

            return "开始全部类型" + typeSet + "账户";
        }));

        CommandMenuNode refresh = new CommandMenuNode(true, "刷新", "当前账户列表",
                () -> AccountInfoPrinter.printAccountList(getTypedAccountMap()));

        menuNode.addSubMenu(refresh);
        return menuNode;
    }


    /**
     * 打印当前的邀请码
     *
     * @return 邀请码
     */
    private String printCurrentRegisterConfig() {
        String inviteCode = botConfig.getConfigMap().get(INVITE_CODE_KEY);
        String registerType = botConfig.getConfigMap().get(REGISTER_TYPE_KEY);

        return "(当前邀请码为:" + inviteCode + ")\n"
                + "(当前注册类型为:" + registerType + ")\n";
    }
}
