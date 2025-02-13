package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.TelegramAccount;
import cn.com.helei.bot.core.mvc.mapper.TelegramAccountMapper;
import cn.com.helei.bot.core.mvc.service.ITelegramAccountService;
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
 * @since 2025-02-06
 */
@Slf4j
@Service
public class TelegramAccountServiceImpl extends ServiceImpl<TelegramAccountMapper, TelegramAccount> implements ITelegramAccountService {

    @Autowired
    private ImportService importService;

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

    @Override
    public Result saveTelegrams(List<Map<String, Object>> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        try {
            importService.importTelegramFormRaw(rawLines);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("导入telegram账号失败," + e.getMessage());
        }
    }
}
