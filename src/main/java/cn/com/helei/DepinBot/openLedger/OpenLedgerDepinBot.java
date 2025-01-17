package cn.com.helei.DepinBot.openLedger;

import cn.com.helei.DepinBot.core.AbstractDepinWSClient;
import cn.com.helei.DepinBot.core.CommandLineDepinBot;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.dto.AccountContext;


/**
 * OpenLedger depin 项目机器人
 */
public class OpenLedgerDepinBot extends CommandLineDepinBot<String, String> {

    private final OpenLedgerConfig openLedgerConfig;

    public OpenLedgerDepinBot(OpenLedgerConfig openLedgerConfig) {
        super(openLedgerConfig);
        this.openLedgerConfig = openLedgerConfig;
    }

    @Override
    protected CommandMenuNode buildMenuNode() {
        CommandMenuNode menuNode = new CommandMenuNode("主菜单", "这是主菜单，请选择", null);
        CommandMenuNode menuNodeA = new CommandMenuNode("子菜单A", "这是子菜单A，请选择", null);
        CommandMenuNode menuNodeB = new CommandMenuNode("子菜单B", "这是子菜单B，请选择", null);

        menuNodeA.addSubMenu(new CommandMenuNode("子菜单A-1", "这是子菜单A-1，请选择", ()->"haha进入了A-1"));
        menuNodeA.addSubMenu(new CommandMenuNode("子菜单A-2", "这是子菜单A-2，请选择", ()->"haha进入了A-2"));

        menuNodeB.addSubMenu(new CommandMenuNode("子菜单B-1", "这是子菜单B-1，请选择", null));
        menuNodeB.addSubMenu(new CommandMenuNode("子菜单B-2", "这是子菜单B-2，请选择", null));

        menuNode.addSubMenu(menuNodeA);
        menuNode.addSubMenu(menuNodeB);

        return menuNode;
    }

    @Override
    public AbstractDepinWSClient<String, String> buildAccountWSClient(AccountContext accountContext) {
        return new OpenLedgerDepinWSClient(accountContext);
    }
}
