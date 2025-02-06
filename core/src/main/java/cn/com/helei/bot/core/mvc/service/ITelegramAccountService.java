package cn.com.helei.bot.core.mvc.service;

import cn.com.helei.bot.core.entity.TelegramAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-06
 */
public interface ITelegramAccountService extends IService<TelegramAccount> {

    Integer insertOrUpdate(TelegramAccount telegramAccount);

    Integer insertOrUpdateBatch(List<TelegramAccount> telegramAccounts);

}
