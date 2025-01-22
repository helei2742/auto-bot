package cn.com.helei.DepinBot.core.pool.account;

import cn.com.helei.DepinBot.core.pool.AbstractYamlLinePool;

import java.util.List;

public class AccountPool extends AbstractYamlLinePool<DepinClientAccount> {

    public AccountPool() {
        super(DepinClientAccount.class);
    }


    public static AccountPool getDefault() {
        return loadYamlPool(
                "bot/account.yaml",
                "bot.account",
                AccountPool.class
        );
    }

    public static void main(String[] args) {
        AccountPool aDefault = getDefault();
        System.out.println(aDefault.printPool());
    }
}
