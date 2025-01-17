package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import cn.com.helei.DepinBot.openLedger.OpenLedgerConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class CommandLineDepinBotTest {

    @Test
    public void testMenu() throws IOException, InterruptedException, DepinBotStartException {
        CommandLineDepinBot commandLineDepinBot = new CommandLineDepinBot<String, String>(OpenLedgerConfig.loadYamlConfig("app/openledger.yaml")) {

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
                return null;
            }
        };

        commandLineDepinBot.start();
    }
}
