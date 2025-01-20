package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;


/**
 * 默认菜单构造器
 */
public class DefaultCommandMenuBuilder {

    private final CommandLineDepinBot<?,?> commandLineDepinBot;

    public DefaultCommandMenuBuilder(CommandLineDepinBot<?,?> commandLineDepinBot) {
        this.commandLineDepinBot = commandLineDepinBot;
    }


    /**
     * 添加默认菜单节点，并返回添加后的菜单
     *
     * @param menuNode 原菜单节点
     * @return 添加默认菜单节点后的菜单
     */
    public CommandMenuNode addDefaultMenuNode(CommandMenuNode menuNode) {
        return menuNode.addSubMenu(buildProxyListMenuNode())
                .addSubMenu(buildBrowserListMenuNode())
                .addSubMenu(buildAccountListMenuNode())
                .addSubMenu(buildStartAccountConnectMenuNode());
    }

    /**
     * 构建查看代理列表的菜单节点
     *
     * @return 查看代理列表菜单节点
     */
    private CommandMenuNode buildProxyListMenuNode() {
        return new CommandMenuNode(
                "查看代理列表",
                "当前代理列表文件:" + commandLineDepinBot.getProxyPool().getConfigClassPath(),
                commandLineDepinBot.getProxyPool()::printPool
        );
    }

    /**
     * 构建查看浏览器环境列表的菜单节点
     *
     * @return 查看浏览器环境列表菜单节点
     */
    private CommandMenuNode buildBrowserListMenuNode() {
        return new CommandMenuNode(
                "查看浏览器环境列表",
                "当前代理列表文件:" + commandLineDepinBot.getBrowserEnvPool().getConfigClassPath(),
                commandLineDepinBot.getBrowserEnvPool()::printPool
        );
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
                commandLineDepinBot.getAccountContextManager()::printAccountList
        );

        return accountListMenuNode.addSubMenu(buildAccountRewardMenuNode());
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
                commandLineDepinBot.getAccountContextManager()::printAccountReward
        );
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
                () -> commandLineDepinBot.startAccountDepinClient() + "\n"
                        + commandLineDepinBot.getAccountContextManager().printAccountList()
        );

        CommandMenuNode refresh = new CommandMenuNode(true,"刷新", "当前账户列表",
                commandLineDepinBot.getAccountContextManager()::printAccountList);

        menuNode.addSubMenu(refresh);
        return menuNode;
    }

}
