package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.entity.DiscordAccount;
import cn.com.helei.bot.core.mvc.mapper.DiscordAccountMapper;
import cn.com.helei.bot.core.mvc.service.IDiscordAccountService;
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
public class DiscordAccountServiceImpl extends ServiceImpl<DiscordAccountMapper, DiscordAccount> implements IDiscordAccountService {
    @Override
    public Integer insertOrUpdate(DiscordAccount discordAccount) {
        discordAccount.setInsertDatetime(LocalDateTime.now());
        discordAccount.setUpdateDatetime(LocalDateTime.now());
        discordAccount.setIsValid(1);

        return baseMapper.insertOrUpdate(discordAccount);
    }

    @Override
    public Integer insertOrUpdateBatch(List<DiscordAccount> browserEnvs) {
        int successCount = 0;
        for (DiscordAccount discordAccount : browserEnvs) {
            try {
                Integer count = insertOrUpdate(discordAccount);
                successCount += count == null ? 0 : count;;
            } catch (Exception e) {
                log.error("insert or update [{}] error", discordAccount, e);
            }
        }

        return successCount;
    }
}
