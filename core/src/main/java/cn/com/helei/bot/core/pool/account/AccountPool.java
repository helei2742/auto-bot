package cn.com.helei.bot.core.pool.account;

import cn.com.helei.bot.core.pool.AbstractYamlLinePool;

public class AccountPool extends AbstractYamlLinePool<DepinClientAccount> {

    public AccountPool() {
        super(DepinClientAccount.class);
    }


    public static AccountPool getDefault() {
        return loadYamlPool(
                "config/bot/account.yaml",
                "bot.account",
                AccountPool.class
        );
    }

    public static void main(String[] args) {
        AccountPool aDefault = getDefault();
        System.out.println(aDefault.printPool());
    }
}
