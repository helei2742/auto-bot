

depin:
openledger:
browserEnvPoolConfig: browser-env.yaml
networkPoolConfig: network-proxy.yaml

wsBaseUrl: wss://apitn.openledger.xyz/ws/v1/orch
origin: chrome-extension://ekbbplmjjgoobhdlffmgeokalelnmjjc
openLedgerAccounts:
        - name: 914577981@qq.com
token: 123
proxyId: 1
browserEnvId: 1


depin:
browser:
envs:
        - id: 1
headers:
User-Agent:
Cache-Control:  no-cache
Accept-Language: zh-CN,zh;q=0.9,en;q=0.8


depin:
network:
proxy:
pool:
        - id: 1
host: 172.0.0.1
port: 12135
username: 12321
password: 412312
        - id: 2
host: 172.0.0.2
port: 12135
username: 12321
password: 412312
        - id: 3
host: 172.0.0.3
port: 12135
username: 12321
password: 412312
        - id: 4
host: 172.0.0.3
port: 12135
username: 12321
password: 412312

        package cn.com.helei.depin.depin;

import cn.com.helei.depin.app.openLedger.OpenLedgerConfig;
import cn.com.helei.depin.core.CommandLineDepinBot;
import cn.com.helei.depin.core.command.CommandMenuNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;


class CommandLineDepinBotTest {


    @Test
    public void testMenu() throws IOException, InterruptedException {
        CommandLineDepinBot commandLineDepinBot = new CommandLineDepinBot(OpenLedgerConfig.loadYamlConfig("app/openledger.yaml")) {

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
        };

        commandLineDepinBot.start();
    }
}









