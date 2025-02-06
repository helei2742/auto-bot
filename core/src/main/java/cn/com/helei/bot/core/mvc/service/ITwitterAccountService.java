package cn.com.helei.bot.core.mvc.service;

import cn.com.helei.bot.core.entity.TwitterAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface ITwitterAccountService extends IService<TwitterAccount> {

    Integer insertOrUpdate(TwitterAccount twitterAccount);

    Integer insertOrUpdateBatch(List<TwitterAccount> twitterAccounts);

}
