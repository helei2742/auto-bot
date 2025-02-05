package cn.com.helei.bot.core.bot.base;


import cn.com.helei.bot.core.entity.AccountContext;

import javax.mail.Message;
import java.util.concurrent.CompletableFuture;

public interface AccountAutoBot {

    /**
     * 注册账户
     *
     * @param accountContext accountContext
     * @param inviteCode     inviteCode
     * @return CompletableFuture<Boolean> 是否注册成功
     */
    CompletableFuture<Boolean> registerAccount(AccountContext accountContext, String inviteCode);

    /**
     * 从message提取需要的内容验证邮箱
     *
     * @param accountContext accountContext
     * @param message        message
     * @return CompletableFuture<Boolean>
     */
    CompletableFuture<Boolean> verifierAccountEmail(AccountContext accountContext, Message message);

    /**
     * 请求获取账户token
     *
     * @param accountContext accountContext
     * @return CompletableFuture<String> token
     */
    CompletableFuture<String> requestTokenOfAccount(AccountContext accountContext);


    /**
     * 开始账户自动收获,会自动循环。返回false则跳出自动循环
     *
     * @return CompletableFuture<Void>
     */
    boolean doAccountClaim(AccountContext accountContext);


    /**
     * 更新账户奖励信息
     *
     * @param accountContext accountContext
     * @return CompletableFuture<Boolean>
     */
    CompletableFuture<Boolean> updateAccountRewordInfo(AccountContext accountContext);

}
