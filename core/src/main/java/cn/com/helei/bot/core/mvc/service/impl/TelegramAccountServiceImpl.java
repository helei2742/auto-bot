package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.TelegramAccount;
import cn.com.helei.bot.core.mvc.mapper.TelegramAccountMapper;
import cn.com.helei.bot.core.mvc.service.ITelegramAccountService;
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
 * @since 2025-02-06
 */
@Slf4j
@Service
public class TelegramAccountServiceImpl extends ServiceImpl<TelegramAccountMapper, TelegramAccount> implements ITelegramAccountService {
    @Override
    public Integer insertOrUpdate(TelegramAccount telegramAccount) {
        telegramAccount.setInsertDatetime(LocalDateTime.now());
        telegramAccount.setUpdateDatetime(LocalDateTime.now());
        telegramAccount.setIsValid(1);

        return baseMapper.insertOrUpdate(telegramAccount);
    }

    @Override
    public Integer insertOrUpdateBatch(List<TelegramAccount> browserEnvs) {
        int successCount = 0;
        for (TelegramAccount telegramAccount : browserEnvs) {
            try {
                Integer count = insertOrUpdate(telegramAccount);
                successCount += count == null ? 0 : count;;
            } catch (Exception e) {
                log.error("insert or update [{}] error", telegramAccount, e);
            }
        }

        return successCount;
    }
}
