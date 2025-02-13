package cn.com.helei.bot.core.mvc.service;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.AccountContext;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface IBotAccountContextService extends IService<AccountContext> {
    Integer insertOrUpdate(AccountContext accountBaseInfo);

    Integer insertOrUpdateBatch(List<AccountContext> twitterAccounts);

    Result saveBotAccountContext(Integer botId, String botKey, List<Map<String, Object>> acKVMap);
}
