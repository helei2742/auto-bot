package cn.com.helei.DepinBot.openLedger;

import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;

import static org.junit.jupiter.api.Assertions.*;

class OpenLedgerDepinBotTest {

    @Test
    public void test() throws DepinBotStartException {
        String configClasspath = "app/openLedger.yaml";

        OpenLedgerDepinBot openLedgerDepinBot = new OpenLedgerDepinBot(OpenLedgerConfig.loadYamlConfig(configClasspath));

        openLedgerDepinBot.start();
    }

    @Test
    public void testProxy() {
        // 代理信息
        String proxyHost = "150.241.112.100";  // 代理服务器地址
        int proxyPort = 6104;                  // 代理服务器端口
        String proxyUser = "hldjmuos";    // 代理用户名
        String proxyPass = "545n41b7z20x";    // 代理密码
        String testUrl = "http://www.google.com"; // 目标URL

// 设置代理
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", String.valueOf(proxyPort));

        // 设置代理认证
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
            }
        });

        // 设置连接超时和读取超时
        int connectionTimeout = 5000;  // 连接超时（毫秒）
        int readTimeout = 5000;        // 读取超时（毫秒）

        // 测试连接
        try {
            URL url = new URL(testUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);

            // 获取响应码
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Proxy connection is working.");
            } else {
                System.out.println("Failed to connect via proxy. Response Code: " + responseCode);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
