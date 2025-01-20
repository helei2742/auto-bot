package cn.com.helei.DepinBot.openLedger;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.dto.DepinClientAccount;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@ToString
public class OpenLedgerConfig extends BaseDepinBotConfig {


    /**
     * 浏览器标识
     */
    private String origin;

    /**
     * 账户列表
     */
    private List<OpenLedgerAccount> openLedgerAccounts;


    public static void main(String[] args) {
        OpenLedgerConfig openLedgerConfig = loadYamlConfig("app/openledger.yaml");
        System.out.println(openLedgerConfig);
    }

    public static OpenLedgerConfig loadYamlConfig(String classpath) {
        Yaml yaml = new Yaml();
        log.info("开始加载OpenLedger配置信息-file classpath:[{}}", classpath);
        try (InputStream inputStream = OpenLedgerConfig.class.getClassLoader().getResourceAsStream(classpath)) {
            Map<String, Object> yamlData = yaml.load(inputStream);
            Map<String, Object> depin = (Map<String, Object>) yamlData.get("depin");
            Map<String, Object> openledger = (Map<String, Object>) depin.get("openledger");

            //Step 1 基础配置文件
            OpenLedgerConfig openLedgerConfig = yaml.loadAs(yaml.dump(openledger), OpenLedgerConfig.class);

            //Step 4 账户列表完善
            openLedgerConfig.getOpenLedgerAccounts().forEach(openLedgerAccount -> {
                openLedgerAccount.setOpenLedgerConfig(openLedgerConfig);
            });

            log.info("OpenLedger配置信息加载完毕: 共{}个账号", openLedgerConfig.getOpenLedgerAccounts().size());

            return openLedgerConfig;
        } catch (IOException e) {
            throw new RuntimeException(String.format("价值配置网络代理池文件[%s]发生错误", classpath));
        }
    }

    @Override
    public List<DepinClientAccount> getAccountList() {
        return openLedgerAccounts.stream().map(openLedgerAccount -> (DepinClientAccount) openLedgerAccount).toList();
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class OpenLedgerAccount extends DepinClientAccount {

        private final static String printTemplate = "%-25s\t%-25s\t%-5s";

        private transient OpenLedgerConfig openLedgerConfig;

        private String token;

        private String identity;

        private String ownerAddress;


        public static String printTittle() {
            return String.format(printTemplate, "账户名", "代理", "环境ID");
        }

        @Override
        public String getConnectUrl() {
            return openLedgerConfig.getWsBaseUrl() + "?authToken=" + token;
        }

        @Override
        public HttpHeaders getWSHeaders() {
            HttpHeaders headers = new DefaultHttpHeaders();

            headers.add("Upgrade", "websocket")
                    .add("Origin", openLedgerConfig.origin)
                    .add("Host", "apitn.openledger.xyz")
                    .add("Connection", "Upgrade")
            ;
            return headers;
        }

        @Override
        public HttpHeaders getRestHeaders() {
            HttpHeaders restHeaders = super.getRestHeaders();

            restHeaders.add("authorization", "Bearer " + token);

            return restHeaders;
        }

        @Override
        public String toString() {
            return "OpenLedgerAccount{" +
                    "token='" + token + '\'' +
                    ", identity='" + identity + '\'' +
                    ", ownerAddress='" + ownerAddress + '\'' +
                    "} " + super.toString();
        }
    }
}
