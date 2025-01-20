package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import cn.com.helei.DepinBot.openLedger.OpenLedgerConfig;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

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

            @Override
            public void whenAccountConnected(AccountContext accountContext, Boolean success) {

            }
        };

        commandLineDepinBot.start();
    }


    @Test
    public void testReward() {


        OkHttpClient client = new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("179.61.172.202", 6753)))
                .proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic("hldjmuos", "545n41b7z20x");
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });

        client = builder.build();
        Request request = new Request.Builder()
                .url("https://rewardstn.openledger.xyz/api/v1/reward")
                .header("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZGRyZXNzIjoiMHgzMmVhZDg0NWRkNGQyY2U0NWQ1ZmFiODFiZDBmOTdjMWI0M2U4OTY0IiwiaWQiOjAsImV4cCI6MTc2ODY3MzUwM30.t1RjJbh225Ljixm9O_KQixNXIK2elz_JDh36u-gI8Uw")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            System.out.println(response.body().string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
