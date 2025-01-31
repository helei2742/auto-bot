package cn.com.helei.bot.core.supporter;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.dto.RewordInfo;
import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.dto.account.AccountPrintDto;
import cn.com.helei.bot.core.pool.env.BrowserEnv;
import cn.com.helei.bot.core.pool.network.NetworkProxy;
import cn.com.helei.bot.core.util.table.CommandLineTablePrintHelper;

import java.util.List;
import java.util.Map;

public class AccountInfoPrinter {

    /**
     * 打印账号列表
     *
     * @return String
     */
    public static String printAccountList(Map<String, List<AccountContext>> typedAccountMap) {

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<AccountContext>> entry : typedAccountMap.entrySet()) {
            String type = entry.getKey();
            List<AccountContext> accountContexts = entry.getValue();

            sb.append(type).append(" 账户列表\n");

            List<AccountPrintDto> list = accountContexts.stream().map(accountContext -> {
                NetworkProxy proxy = accountContext.getProxy();
                BrowserEnv browserEnv = accountContext.getBrowserEnv();

                return AccountPrintDto
                        .builder()
                        .id(accountContext.getAccountBaseInfo().getId())
                        .name(accountContext.getName())
                        .proxyInfo(proxy == null ? "NO_PROXY" : proxy.getId() + "-" + proxy.getAddressStr())
                        .browserEnvInfo(String.valueOf(browserEnv == null ? "NO_ENV" : browserEnv.getId()))
                        .signUp(accountContext.getAccountBaseInfo().getSignUp())
                        .build();
            }).toList();

            sb.append(CommandLineTablePrintHelper.generateTableString(list, AccountPrintDto.class)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 打印账户连接情况
     *
     * @return String
     */
    public static String printAccountConnectStatusList(Map<String, List<AccountContext>> typedAccountMap) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<AccountContext>> entry : typedAccountMap.entrySet()) {
            String type = entry.getKey();
            List<AccountContext> accountContexts = entry.getValue();

            sb.append(type).append(" 账号链接状态列表:\n");

            List<ConnectStatusInfo> list = accountContexts.stream()
                    .map(AccountContext::getConnectStatusInfo).toList();

            sb.append(CommandLineTablePrintHelper.generateTableString(list, ConnectStatusInfo.class)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 打印账号收益
     *
     * @return String
     */
    public static String printAccountReward(Map<String, List<AccountContext>> typedAccountMap) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<AccountContext>> entry : typedAccountMap.entrySet()) {
            String type = entry.getKey();
            List<AccountContext> accountContexts = entry.getValue();

            sb.append(type).append(" 收益列表:\n");

            List<RewordInfo> list = accountContexts.stream()
                    .map(accountContext -> accountContext.getRewordInfo().newInstance()).toList();

            sb.append(CommandLineTablePrintHelper.generateTableString(list, RewordInfo.class)).append("\n");
        }

        return sb.toString();
    }

}
