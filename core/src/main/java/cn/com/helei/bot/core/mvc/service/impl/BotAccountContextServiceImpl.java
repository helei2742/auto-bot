package cn.com.helei.bot.core.mvc.service.impl;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.AccountContext;
import cn.com.helei.bot.core.mvc.mapper.BotAccountContextMapper;
import cn.com.helei.bot.core.mvc.service.IBotAccountContextService;
import cn.com.helei.bot.core.supporter.botapi.ImportService;
import cn.hutool.core.util.StrUtil;
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
public class BotAccountContextServiceImpl extends ServiceImpl<BotAccountContextMapper, AccountContext> implements IBotAccountContextService {

    @Autowired
    private ImportService importService;

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

    @Override
    public Result saveBotAccountContext(Integer botId, String botKey, List<Map<String, Object>> rawLines) {
        if (botId == null || StrUtil.isBlank(botKey)) {
            return Result.fail("botId或botKey不能为空");
        }

        try {
            importService.importBotAccountContextFromRaw(botId, botKey, rawLines);

            return Result.ok();
        } catch (Exception e) {
            log.error("botId[{}]-botKey[{}] 报错账户信息失败", botId, botKey, e);
            return Result.fail("保存失败, " + e.getMessage());
        }
    }
}
