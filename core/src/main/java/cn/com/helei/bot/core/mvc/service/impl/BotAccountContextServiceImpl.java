package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.mvc.mapper.BotAccountContextMapper;
import cn.com.helei.bot.core.mvc.service.IBotAccountContextService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Slf4j
@Service
public class BotAccountContextServiceImpl extends ServiceImpl<BotAccountContextMapper, AccountContext> implements IBotAccountContextService {

    @Override
    public Integer insertOrUpdate(AccountContext accountBaseInfo) {
        accountBaseInfo.setInsertDatetime(LocalDateTime.now());
        accountBaseInfo.setUpdateDatetime(LocalDateTime.now());
        accountBaseInfo.setIsValid(1);

        return baseMapper.insertOrUpdate(accountBaseInfo);
    }

    @Override
    public Integer insertOrUpdateBatch(List<AccountContext> accountContext) {
        int successCount = 0;
        for (AccountContext accountBaseInfo : accountContext) {
            try {
                Integer count = insertOrUpdate(accountBaseInfo);
                successCount += count == null ? 0 : count;;
            } catch (Exception e) {
                log.error("insert or update [{}] error", accountBaseInfo, e);
            }
        }

        return successCount;
    }
}
