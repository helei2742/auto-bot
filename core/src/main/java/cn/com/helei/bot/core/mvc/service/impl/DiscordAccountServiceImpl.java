package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.DiscordAccount;
import cn.com.helei.bot.core.mvc.mapper.DiscordAccountMapper;
import cn.com.helei.bot.core.mvc.service.IDiscordAccountService;
import cn.com.helei.bot.core.supporter.botapi.ImportService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private ImportService importService;

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

    @Override
    public Result saveDiscordAccounts(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importService.importDiscordFromRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("导入discord账号失败," + e.getMessage());
        }
    }
}
