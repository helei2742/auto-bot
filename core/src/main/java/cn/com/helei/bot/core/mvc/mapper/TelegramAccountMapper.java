package cn.com.helei.bot.core.mvc.mapper;

import cn.com.helei.bot.core.entity.TelegramAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author com.helei
 * @since 2025-02-06
 */
public interface TelegramAccountMapper extends BaseMapper<TelegramAccount> {

    Integer insertOrUpdate(TelegramAccount telegramAccount);

}
