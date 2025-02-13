package cn.com.helei.bot.core.mvc.service;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.DiscordAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

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

    Result saveDiscordAccounts(List<Map<String, Object>> rawLines);
}
