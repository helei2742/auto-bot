package cn.com.helei.bot.core.mvc.service;

import cn.com.helei.bot.core.entity.DiscordAccount;
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
public interface IDiscordAccountService extends IService<DiscordAccount> {

    Integer insertOrUpdate(DiscordAccount discordAccount);

    Integer insertOrUpdateBatch(List<DiscordAccount> discordAccounts);

}
